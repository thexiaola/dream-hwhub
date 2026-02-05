package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminUserServiceTest {

    @Autowired
    private IUserService userService;

    @Test
    public void testAdminCreateUser() {
        // 创建测试用户
        User user = new User();
        user.setUserNo("TEST001");
        user.setUsername("测试用户");
        user.setEmail("test001@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);

        User createdUser = userService.adminCreateUser(user);
        
        assertNotNull(createdUser.getId());
        assertEquals("TEST001", createdUser.getUserNo());
        assertEquals("测试用户", createdUser.getUsername());
        assertEquals("test001@example.com", createdUser.getEmail());
        assertEquals(Short.valueOf((short) 10), createdUser.getPermission());
        assertNotNull(createdUser.getPassword());
        assertNotEquals("123456", createdUser.getPassword()); // 密码应该被加密
    }

    @Test
    public void testAdminUpdateUser() {
        // 先创建用户
        User user = new User();
        user.setUserNo("TEST002");
        user.setUsername("测试用户2");
        user.setEmail("test002@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        
        User createdUser = userService.adminCreateUser(user);
        
        // 更新用户信息
        User updateUser = new User();
        updateUser.setUsername("更新后的用户名");
        updateUser.setPermission((short) 20);
        // 不设置密码，应该保持原密码
        
        User updatedUser = userService.adminUpdateUser(createdUser.getId(), updateUser);
        
        assertEquals("更新后的用户名", updatedUser.getUsername());
        assertEquals(Short.valueOf((short) 20), updatedUser.getPermission());
        assertEquals(createdUser.getEmail(), updatedUser.getEmail()); // 邮箱应该不变
    }

    @Test
    public void testAdminDeleteUser() {
        // 先创建用户
        User user = new User();
        user.setUserNo("TEST003");
        user.setUsername("测试用户3");
        user.setEmail("test003@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        
        User createdUser = userService.adminCreateUser(user);
        Integer userId = createdUser.getId();
        
        // 删除用户
        boolean result = userService.adminDeleteUser(userId);
        assertTrue(result);
        
        // 验证用户已删除
        User deletedUser = userService.getById(userId);
        assertNull(deletedUser);
    }

    @Test
    public void testAdminListUsers() {
        // 查询用户列表（即使没有数据也应该返回空列表而不是null）
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setPage(1);
        queryRequest.setSize(30);
        
        PageResult<User> pageResult = userService.adminListUsers(queryRequest);
        
        assertNotNull(pageResult);
        assertNotNull(pageResult.getRecords());
        assertEquals(Integer.valueOf(1), pageResult.getPage());
        assertEquals(Integer.valueOf(30), pageResult.getSize());
    }

    @Test
    public void testAdminListUsersWithKeywordSearch() {
        // 创建测试用户
        User user = new User();
        user.setUserNo("SEARCH001");
        user.setUsername("搜索测试用户");
        user.setEmail("search@test.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        userService.adminCreateUser(user);
        
        // 使用关键词搜索
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setKeyword("搜索");
        queryRequest.setPage(1);
        queryRequest.setSize(10);
        
        PageResult<User> pageResult = userService.adminListUsers(queryRequest);
        
        assertNotNull(pageResult);
        assertFalse(pageResult.getRecords().isEmpty());
        
        // 验证搜索结果包含关键词
        boolean found = false;
        for (User u : pageResult.getRecords()) {
            if (u.getUsername().contains("搜索")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testAdminListUsersWithPermissionFilter() {
        // 创建不同权限的测试用户
        User user1 = new User();
        user1.setUserNo("PERM001");
        user1.setUsername("权限测试用户1");
        user1.setEmail("perm1@test.com");
        user1.setPassword("123456");
        user1.setPermission((short) 10);
        userService.adminCreateUser(user1);
        
        User user2 = new User();
        user2.setUserNo("PERM002");
        user2.setUsername("权限测试用户2");
        user2.setEmail("perm2@test.com");
        user2.setPassword("123456");
        user2.setPermission((short) 50);
        userService.adminCreateUser(user2);
        
        // 按权限筛选
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setPermission((short) 10);
        queryRequest.setPage(1);
        queryRequest.setSize(10);
        
        PageResult<User> pageResult = userService.adminListUsers(queryRequest);
        
        assertNotNull(pageResult);
        assertFalse(pageResult.getRecords().isEmpty());
        
        // 验证所有结果都是权限10的用户
        for (User u : pageResult.getRecords()) {
            assertEquals(Short.valueOf((short) 10), u.getPermission());
        }
    }
}