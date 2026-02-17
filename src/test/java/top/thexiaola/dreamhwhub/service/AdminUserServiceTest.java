package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;
import top.thexiaola.dreamhwhub.module.adminuser.service.AdminUserService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminUserServiceTest {

    @Autowired
    private AdminUserService userService;

    @Test
    public void testAdminCreateUser() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 创建目标用户（使用UUID确保唯一性）
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserNo("TEST_CREATE_" + uniqueSuffix);
        user.setUsername("测试用户_" + uniqueSuffix);
        user.setEmail("test_create_" + uniqueSuffix + "@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);

        User createdUser = userService.adminCreateUser(currentUser, user);
        
        assertNotNull(createdUser.getId());
        assertEquals("TEST_CREATE_" + uniqueSuffix, createdUser.getUserNo());
        assertEquals("测试用户_" + uniqueSuffix, createdUser.getUsername());
        assertEquals("test_create_" + uniqueSuffix + "@example.com", createdUser.getEmail());
        assertEquals(Short.valueOf((short) 10), createdUser.getPermission());
        assertNotNull(createdUser.getPassword());
        assertNotEquals("123456", createdUser.getPassword()); // 密码应该被加密
    }

    @Test
    public void testAdminUpdateUser() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 先创建用户（使用UUID确保唯一性）
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserNo("TEST_UPDATE_" + uniqueSuffix);
        user.setUsername("测试用户2_" + uniqueSuffix);
        user.setEmail("test_update_" + uniqueSuffix + "@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        
        User createdUser = userService.adminCreateUser(currentUser, user);
        
        // 更新用户信息
        User updateUser = new User();
        updateUser.setUsername("更新后的用户名_" + uniqueSuffix);
        updateUser.setPermission((short) 20);
        // 不设置密码，应该保持原密码
        
        User updatedUser = userService.adminUpdateUser(currentUser, createdUser.getId(), updateUser);
        
        assertEquals("更新后的用户名_" + uniqueSuffix, updatedUser.getUsername());
        assertEquals(Short.valueOf((short) 20), updatedUser.getPermission());
        assertEquals(createdUser.getEmail(), updatedUser.getEmail()); // 邮箱应该不变
    }

    @Test
    public void testAdminDeleteUser() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 先创建用户（使用UUID确保唯一性）
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserNo("TEST_DELETE_" + uniqueSuffix);
        user.setUsername("测试用户3_" + uniqueSuffix);
        user.setEmail("test_delete_" + uniqueSuffix + "@example.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        
        User createdUser = userService.adminCreateUser(currentUser, user);
        Integer userId = createdUser.getId();
        
        // 删除用户
        boolean result = userService.adminDeleteUser(currentUser, userId);
        assertTrue(result);
        
        // 验证用户已删除
        User deletedUser = userService.getById(userId);
        assertNull(deletedUser);
    }

    @Test
    public void testAdminListUsers() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 查询用户列表（即使没有数据也应该返回空列表而不是null）
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setPage(1);
        queryRequest.setSize(30);
        
        PageResult<User> pageResult = userService.adminListUsers(currentUser, queryRequest);
        
        assertNotNull(pageResult);
        assertNotNull(pageResult.getRecords());
        assertEquals(Integer.valueOf(1), pageResult.getPage());
        assertEquals(Integer.valueOf(30), pageResult.getSize());
    }

    @Test
    public void testAdminListUsersWithKeywordSearch() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 创建测试用户（使用UUID确保唯一性）
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserNo("SEARCH_" + uniqueSuffix);
        user.setUsername("搜索测试用户_" + uniqueSuffix);
        user.setEmail("search_" + uniqueSuffix + "@test.com");
        user.setPassword("123456");
        user.setPermission((short) 10);
        userService.adminCreateUser(currentUser, user);
        
        // 使用关键词搜索
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setKeyword(uniqueSuffix); // 使用唯一后缀作为搜索关键词
        queryRequest.setPage(1);
        queryRequest.setSize(10);
        
        PageResult<User> pageResult = userService.adminListUsers(currentUser, queryRequest);
        
        assertNotNull(pageResult);
        assertFalse(pageResult.getRecords().isEmpty());
        
        // 验证搜索结果包含关键词
        boolean found = false;
        for (User u : pageResult.getRecords()) {
            if (u.getUsername().contains(uniqueSuffix)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testAdminListUsersWithPermissionFilter() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 创建不同权限的测试用户（使用UUID确保唯一性）
        String uniqueSuffix1 = UUID.randomUUID().toString().substring(0, 8);
        String uniqueSuffix2 = UUID.randomUUID().toString().substring(0, 8);
        
        User user1 = new User();
        user1.setUserNo("PERM1_" + uniqueSuffix1);
        user1.setUsername("权限测试用户1_" + uniqueSuffix1);
        user1.setEmail("perm1_" + uniqueSuffix1 + "@test.com");
        user1.setPassword("123456");
        user1.setPermission((short) 10);
        userService.adminCreateUser(currentUser, user1);
        
        User user2 = new User();
        user2.setUserNo("PERM2_" + uniqueSuffix2);
        user2.setUsername("权限测试用户2_" + uniqueSuffix2);
        user2.setEmail("perm2_" + uniqueSuffix2 + "@test.com");
        user2.setPassword("123456");
        user2.setPermission((short) 50);
        userService.adminCreateUser(currentUser, user2);
        
        // 按权限筛选
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setPermission((short) 10);
        queryRequest.setPage(1);
        queryRequest.setSize(10);
        
        PageResult<User> pageResult = userService.adminListUsers(currentUser, queryRequest);
        
        assertNotNull(pageResult);
        assertFalse(pageResult.getRecords().isEmpty());
        
        // 验证所有结果都是权限10的用户
        for (User u : pageResult.getRecords()) {
            assertEquals(Short.valueOf((short) 10), u.getPermission());
        }
    }
}