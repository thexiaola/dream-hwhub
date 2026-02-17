package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.thexiaola.dreamhwhub.domain.InvitationCode;
import top.thexiaola.dreamhwhub.domain.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InvitationCodeServiceTest {

    @Autowired
    private IInvitationCodeService invitationCodeService;
    
    @Autowired
    private IUserService userService;

    @Test
    public void testGenerateAndValidateInvitationCode() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        List<String> codes = invitationCodeService.generateInvitationCodes(currentUser, 3, 7);
        
        assertNotNull(codes);
        assertEquals(3, codes.size());
        
        String code = codes.getFirst();
        assertEquals(8, code.length());
        
        // 验证邀请码
        boolean isValid = invitationCodeService.validateInvitationCode(code);
        assertTrue(isValid);
        
        // 测试使用邀请码
        boolean used = invitationCodeService.useInvitationCode(code);
        assertTrue(used);
        
        // 再次验证应该无效（因为每个邀请码只能用一次）
        isValid = invitationCodeService.validateInvitationCode(code);
        assertFalse(isValid);
    }

    @Test
    public void testInvalidInvitationCode() {
        // 测试不存在的邀请码
        boolean isValid = invitationCodeService.validateInvitationCode("INVALID123");
        assertFalse(isValid);
    }

    @Test
    public void testDeleteInvitationCode() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        List<String> codes = invitationCodeService.generateInvitationCodes(currentUser, 1, 7);
        String code = codes.getFirst();
        
        // 删除邀请码
        boolean deleted = invitationCodeService.deleteInvitationCode(currentUser, code);
        assertTrue(deleted);
        
        // 验证删除后的邀请码应该不存在
        boolean isValid = invitationCodeService.validateInvitationCode(code);
        assertFalse(isValid);
    }

    @Test
    public void testGetAllInvitationCodes() {
        // 创建操作用户（权限足够）
        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setPermission((short) 60);
        
        // 生成几个邀请码
        invitationCodeService.generateInvitationCodes(currentUser, 2, 7);
        invitationCodeService.generateInvitationCodes(currentUser, 3, 14);
        
        // 获取所有邀请码
        List<InvitationCode> codes = invitationCodeService.getAllInvitationCodes(currentUser);
        assertTrue(codes.size() >= 5);
        
        // 验证数据正确性
        for (InvitationCode code : codes) {
            assertEquals(Integer.valueOf(1), code.getCreatorId());
            assertNotNull(code.getCode());
            assertNotNull(code.getCreatedTime());
            assertNotNull(code.getExpireTime());
            assertEquals(Integer.valueOf(1), code.getMaxUsage()); // 每个邀请码只能用一次
        }
    }
}