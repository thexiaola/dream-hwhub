package top.thexiaola.dreamhwhub.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUserSearchCondition;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUserSearchRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInvitationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassJoinApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassTeacherApprovalMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassUserInvitationMapper;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 管理员用户检索的条件翻译测试：断言拼出的 SQL 片段与参数占位数量
 *
 * <p>断言包含 ") OR ("、" AND " 与括号数量这类片段格式，与 MyBatis-Plus 生成的 SQL 风格相关；
 * 升级 MyBatis-Plus 后若此类用例变红，先确认是片段格式变化还是真的逻辑回归。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserServiceImplSearchTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ClassInfoMapper classInfoMapper;
    @Mock
    private ClassMemberMapper classMemberMapper;
    @Mock
    private ClassJoinApplicationMapper classJoinApplicationMapper;
    @Mock
    private ClassUserInvitationMapper classUserInvitationMapper;
    @Mock
    private ClassTeacherApprovalMapper classTeacherApprovalMapper;
    @Mock
    private ClassInvitationMapper classInvitationMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    /**
     * 执行一次检索并取回实际下推给数据库的 Wrapper
     */
    private QueryWrapper<User> captureWrapper(List<AdminUserSearchCondition> conditions) {
        doReturn(new Page<User>()).when(userMapper).selectPage(any(), any());
        adminUserService.listUsers(requestOf(conditions), 1, 10);

        ArgumentCaptor<QueryWrapper<User>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(userMapper).selectPage(any(), captor.capture());
        QueryWrapper<User> wrapper = captor.getValue();
        // 取一次 SQL 片段，后续断言读 SQL 文本才拿得到内容
        wrapper.getCustomSqlSegment();
        return wrapper;
    }

    /**
     * 构造只含条件列表的检索请求
     */
    private AdminUserSearchRequest requestOf(List<AdminUserSearchCondition> conditions) {
        AdminUserSearchRequest request = new AdminUserSearchRequest();
        request.setConditions(conditions);
        return request;
    }

    private AdminUserSearchCondition condition(String field, String matchType, String value) {
        AdminUserSearchCondition cond = new AdminUserSearchCondition();
        cond.setField(field);
        cond.setMatchType(matchType);
        cond.setValue(value);
        return cond;
    }

    /**
     * 每个条件只允许产生一个参数占位；占位符重复会把值挤到 ESCAPE 之后，MySQL 报 #1210。
     * 直接数 SQL 中的 #{} 出现次数，不依赖参数 map 的填充时机。
     */
    private void assertSinglePlaceholder(QueryWrapper<User> wrapper) {
        String sql = wrapper.getCustomSqlSegment();
        assertEquals(1, countOf(sql, "#{"),
                "单个条件应只产生一个参数占位，重复的 {0} 会让 MySQL 报 Incorrect arguments to ESCAPE，实际 SQL: " + sql);
    }

    /**
     * 统计子串出现次数
     */
    private int countOf(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    @Test
    @DisplayName("跨表字段模糊匹配：单个参数占位且使用 ! 作为转义符")
    void subQueryFuzzyMatchUsesSinglePlaceholder() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("school", "contains", "第一中学")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        // LIKE 必须落在子查询的 WHERE 内，否则右括号提前闭合会把条件筛到子查询之外
        assertTrue(sql.contains("WHERE s.school_name LIKE #{"),
                "LIKE 应位于子查询 WHERE 内，实际 SQL: " + sql);
        assertFalse(sql.contains(") LIKE"),
                "子查询的右括号不应出现在 LIKE 之前，实际 SQL: " + sql);
        assertTrue(sql.contains("ESCAPE '!'"), "应显式声明转义符，实际 SQL: " + sql);
    }

    @Test
    @DisplayName("跨表字段精确匹配：单个参数占位且不带 ESCAPE")
    void subQueryExactMatchUsesSinglePlaceholder() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("staffNo", "equals", "2024001")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("WHERE staff_no = #{"),
                "= 应位于子查询 WHERE 内，实际 SQL: " + sql);
        assertFalse(sql.contains(") ="),
                "子查询的右括号不应出现在 = 之前，实际 SQL: " + sql);
        assertFalse(sql.contains("ESCAPE"), "精确匹配不应出现 LIKE 的 ESCAPE，实际 SQL: " + sql);
    }

    @Test
    @DisplayName("主表列模糊匹配：单个参数占位")
    void columnFuzzyMatchUsesSinglePlaceholder() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("username", "contains", "the")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("username LIKE #{"), "实际 SQL: " + sql);
        assertTrue(sql.contains("ESCAPE '!'"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("用户输入的 % 与 _ 被转义，不会当作通配符")
    void escapesLikeWildcardsInValue() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("username", "contains", "a%b_c")));

        // 参数值为模式串：! 用于转义，故 % 与 _ 前各补一个 !
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%a!%b!_c%"),
                "实际 SQL: " + wrapper.getCustomSqlSegment()
                        + " 参数: " + wrapper.getParamNameValuePairs());
    }

    @Test
    @DisplayName("用户输入的 ! 转义符本身被加倍，不会破坏转义")
    void escapesEscapeCharacterItself() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("username", "contains", "a!b")));

        assertTrue(wrapper.getParamNameValuePairs().containsValue("%a!!b%"),
                "实际 SQL: " + wrapper.getCustomSqlSegment()
                        + " 参数: " + wrapper.getParamNameValuePairs());
    }

    @Test
    @DisplayName("主表列精确匹配：单个参数占位且不带 ESCAPE")
    void columnExactMatchUsesSinglePlaceholder() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("email", "equals", "a@b.com")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("email = #{"), "实际 SQL: " + sql);
        assertFalse(sql.contains("ESCAPE"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("email 模糊匹配：走主表列 LIKE")
    void emailFuzzyMatch() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("email", "contains", "qq.com")));

        assertSinglePlaceholder(wrapper);
        assertTrue(wrapper.getCustomSqlSegment().contains("email LIKE #{"),
                "实际 SQL: " + wrapper.getCustomSqlSegment());
    }

    @Test
    @DisplayName("姓名精确匹配：子查询内等值比较")
    void realNameExactMatch() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("realName", "equals", "张三")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("WHERE real_name = #{"), "实际 SQL: " + sql);
        assertFalse(sql.contains(") ="), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("班级模糊匹配：子查询内 LIKE 且括号闭合正确")
    void classNameFuzzyMatch() {
        QueryWrapper<User> wrapper = captureWrapper(List.of(condition("className", "contains", "一班")));

        assertSinglePlaceholder(wrapper);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("WHERE c.class_name LIKE #{"), "实际 SQL: " + sql);
        assertFalse(sql.contains(") LIKE"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("字段为空时拒绝，不抛 NPE")
    void nullFieldIsRejected() {
        AdminUserSearchCondition cond = condition(null, "contains", "x");

        assertThrows(BusinessException.class,
                () -> adminUserService.listUsers(requestOf(List.of(cond)), 1, 10));
        // 条件未被翻译成 SQL，不应真的去查库
        verify(userMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("空条件携带的「或者」会传递给下一个有效条件")
    void blankConditionCarriesOrConnector() {
        AdminUserSearchCondition first = condition("username", "contains", "a");
        AdminUserSearchCondition blank = condition("realName", "contains", "   ");
        blank.setConnector("or");
        AdminUserSearchCondition third = condition("email", "contains", "b");
        third.setConnector("and");

        QueryWrapper<User> wrapper = captureWrapper(List.of(first, blank, third));

        String sql = wrapper.getCustomSqlSegment();
        // 空条件被跳过，但它的 OR 必须保留：应为 (A) OR (B)，而不是 (A AND B)
        assertTrue(sql.contains(") OR ("), "空条件携带的 OR 不应丢失，实际 SQL: " + sql);
        assertFalse(sql.contains(" AND "), "两个有效条件应被 OR 分开而非 AND，实际 SQL: " + sql);
        assertEquals(2, countOf(sql, "#{"), "空条件不应产生占位，实际 SQL: " + sql);
    }

    @Test
    @DisplayName("条件列表为 null 时不加任何筛选")
    void nullConditionsProduceNoWhere() {
        doReturn(new Page<User>()).when(userMapper).selectPage(any(), any());
        adminUserService.listUsers(requestOf(null), 1, 10);

        ArgumentCaptor<QueryWrapper<User>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(userMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getCustomSqlSegment();
        assertFalse(sql.contains("WHERE"), "不应产生筛选条件，实际 SQL: " + sql);
        assertEquals(0, countOf(sql, "#{"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("首行连接符为「或者」时按新组起头，不影响结果")
    void leadingOrConnectorStartsFirstGroup() {
        AdminUserSearchCondition first = condition("username", "contains", "a");
        first.setConnector("or");
        AdminUserSearchCondition second = condition("email", "contains", "b");
        second.setConnector("and");

        QueryWrapper<User> wrapper = captureWrapper(List.of(first, second));

        // 首行连接符被忽略，两个条件同组
        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains(") OR ("), "首行的 OR 应被忽略，实际 SQL: " + sql);
        assertEquals(2, countOf(sql, "#{"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("三组条件：组间全部以 OR 连接")
    void threeGroupsJoinWithOr() {
        AdminUserSearchCondition a = condition("username", "contains", "a");
        AdminUserSearchCondition b = condition("email", "contains", "b");
        b.setConnector("or");
        AdminUserSearchCondition c = condition("staffNo", "contains", "c");
        c.setConnector("or");

        QueryWrapper<User> wrapper = captureWrapper(List.of(a, b, c));

        String sql = wrapper.getCustomSqlSegment();
        assertEquals(2, countOf(sql, ") OR ("), "三组之间应有 2 个 OR，实际 SQL: " + sql);
        assertEquals(3, countOf(sql, "#{"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("单组条件不额外嵌套")
    void singleGroupHasNoExtraNesting() {
        AdminUserSearchCondition a = condition("username", "contains", "a");
        AdminUserSearchCondition b = condition("email", "contains", "b");
        b.setConnector("and");

        QueryWrapper<User> wrapper = captureWrapper(List.of(a, b));

        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains(") OR ("), "实际 SQL: " + sql);
        // 单组的条件直接以 AND 相连，不应被额外嵌套（外层那对括号由 MP 自己生成）
        assertTrue(sql.contains("WHERE (username LIKE"),
                "首个条件应紧跟 WHERE 的左括号，实际 SQL: " + sql);
        assertFalse(sql.contains(") AND ("), "条件之间不应各自加括号，实际 SQL: " + sql);
        assertEquals(1, countOf(sql, "("), "单组只应有 MP 生成的一对括号，实际 SQL: " + sql);
        assertEquals(2, countOf(sql, "#{"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("连续「并且」归为一组、组间「或者」：整体包在括号内且条件数不为零")
    void groupsConditionsByConnector() {
        AdminUserSearchCondition first = condition("username", "contains", "a");
        AdminUserSearchCondition second = condition("school", "contains", "b");
        second.setConnector("and");
        AdminUserSearchCondition third = condition("realName", "equals", "c");
        third.setConnector("or");

        QueryWrapper<User> wrapper = captureWrapper(List.of(first, second, third));

        String sql = wrapper.getCustomSqlSegment();
        // 不能用 contains("OR")：ORDER BY 里就含 OR，会恒真；这里校验组间连接形态
        assertTrue(sql.contains(") OR ("), "组间应为 OR，实际 SQL: " + sql);
        assertTrue(sql.contains(" AND "), "组内应为 AND，实际 SQL: " + sql);
        // 3 个条件各一个占位
        assertEquals(3, countOf(sql, "#{"), "实际 SQL: " + sql);
    }

    @Test
    @DisplayName("空白条件被跳过，不产生任何 WHERE 条件")
    void blankConditionsAreSkipped() {
        AdminUserSearchCondition blank = condition("username", "contains", "   ");

        QueryWrapper<User> wrapper = captureWrapper(List.of(blank));

        String sql = wrapper.getCustomSqlSegment();
        assertEquals(0, countOf(sql, "#{"));
        assertFalse(sql.contains("username"));
    }
}
