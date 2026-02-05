package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.thexiaola.dreamhwhub.domain.InvitationCode;

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
        // 创建测试用户（假设已存在权限>50的用户，ID为1）
        List<String> codes = invitationCodeService.generateInvitationCodes(1, 3, 7);
        
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
        List<String> codes = invitationCodeService.generateInvitationCodes(1, 1, 7);
        String code = codes.getFirst();
        
        // 删除邀请码
        boolean deleted = invitationCodeService.deleteInvitationCode(code);
        assertTrue(deleted);
        
        // 验证删除后的邀请码应该不存在
        boolean isValid = invitationCodeService.validateInvitationCode(code);
        assertFalse(isValid);
    }

    @Test
    public void testGetAllInvitationCodes() {
        // 生成几个邀请码
        invitationCodeService.generateInvitationCodes(1, 2, 7);
        invitationCodeService.generateInvitationCodes(1, 3, 14);
        
        // 获取所有邀请码
        List<InvitationCode> codes = invitationCodeService.getAllInvitationCodes();
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