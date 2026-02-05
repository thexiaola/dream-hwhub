package top.thexiaola.dreamhwhub.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.ApiResponse;
import top.thexiaola.dreamhwhub.domain.InvitationCode;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.service.IInvitationCodeService;
import top.thexiaola.dreamhwhub.service.IUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
public class InvitationCodeController {

    private static final Logger logger = LoggerFactory.getLogger(InvitationCodeController.class);
    
    private final IInvitationCodeService invitationCodeService;
    private final IUserService userService;

    public InvitationCodeController(IInvitationCodeService invitationCodeService, IUserService userService) {
        this.invitationCodeService = invitationCodeService;
        this.userService = userService;
    }

    /**
     * 生成邀请码（管理员功能，需要权限等级 > 50）
     */
    @PostMapping("/generate")
    public ApiResponse<List<String>> generateInvitationCodes(
            @RequestParam Integer count,
            @RequestParam(defaultValue = "30") Integer expireDays,
            @RequestAttribute("currentUser") User currentUser,
            HttpServletRequest request) {
        
        LogUtil.info(logger, "收到批量生成邀请码请求，数量: " + count, currentUser, request);
        
        try {
            // 权限检查
            if (currentUser.getPermission() <= 50) {
                LogUtil.warn(logger, "权限不足，无法生成邀请码", currentUser, request);
                return ApiResponse.error(403, "权限不足，需要权限等级大于50");
            }
            
            // 限制单次生成数量
            if (count <= 0 || count > 100) {
                return ApiResponse.error(400, "生成数量必须在1-100之间");
            }
            
            List<String> codes = invitationCodeService.generateInvitationCodes(
                currentUser.getId(), count, expireDays);
            
            LogUtil.info(logger, "批量生成邀请码成功，数量: " + codes.size(), currentUser, request);
            return ApiResponse.success(codes, "邀请码生成成功");
        } catch (Exception e) {
            LogUtil.error(logger, "批量生成邀请码失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "生成邀请码失败: " + e.getMessage());
        }
    }



    /**
     * 查看所有邀请码（管理员功能，需要权限等级 > 50）
     */
    @GetMapping("/all")
    public ApiResponse<List<InvitationCode>> getAllInvitationCodes(
            @RequestAttribute("currentUser") User currentUser,
            HttpServletRequest request) {
        
        LogUtil.info(logger, "收到查看所有邀请码请求", currentUser, request);
        
        try {
            // 权限检查
            if (currentUser.getPermission() <= 50) {
                LogUtil.warn(logger, "权限不足，无法查看所有邀请码", currentUser, request);
                return ApiResponse.error(403, "权限不足，需要权限等级大于50");
            }
            
            List<InvitationCode> codes = invitationCodeService.getAllInvitationCodes();
            LogUtil.info(logger, "获取所有邀请码成功", currentUser, request);
            return ApiResponse.success(codes, "获取成功");
        } catch (Exception e) {
            LogUtil.error(logger, "获取所有邀请码失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "获取所有邀请码失败: " + e.getMessage());
        }
    }

    /**
     * 删除邀请码（管理员功能，需要权限等级 > 50）
     */
    @DeleteMapping("/{code}")
    public ApiResponse<String> deleteInvitationCode(
            @PathVariable String code,
            @RequestAttribute("currentUser") User currentUser,
            HttpServletRequest request) {
        
        LogUtil.info(logger, "收到删除邀请码请求，邀请码: " + code, currentUser, request);
        
        try {
            // 权限检查
            if (currentUser.getPermission() <= 50) {
                LogUtil.warn(logger, "权限不足，无法删除邀请码", currentUser, request);
                return ApiResponse.error(403, "权限不足，需要权限等级大于50");
            }
            
            boolean result = invitationCodeService.deleteInvitationCode(code);
            if (result) {
                LogUtil.info(logger, "邀请码删除成功", currentUser, request);
                return ApiResponse.success(null, "邀请码删除成功");
            } else {
                LogUtil.warn(logger, "邀请码删除失败：邀请码不存在", currentUser, request);
                return ApiResponse.error(400, "邀请码删除失败");
            }
        } catch (Exception e) {
            LogUtil.error(logger, "删除邀请码失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "删除邀请码失败: " + e.getMessage());
        }
    }

    /**
     * 验证邀请码（公开接口，无需登录）
     */
    @GetMapping("/validate/{code}")
    public ApiResponse<Map<String, Object>> validateInvitationCode(
            @PathVariable String code,
            HttpServletRequest request) {
        
        LogUtil.infoAnonymous(logger, "收到验证邀请码请求，邀请码: " + code, request);
        
        try {
            boolean isValid = invitationCodeService.validateInvitationCode(code);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", code);
            result.put("isValid", isValid);
            
            if (isValid) {
                LogUtil.infoAnonymous(logger, "邀请码验证成功", request);
                return ApiResponse.success(result, "邀请码有效");
            } else {
                LogUtil.infoAnonymous(logger, "邀请码验证失败", request);
                return ApiResponse.success(result, "邀请码无效");
            }
        } catch (Exception e) {
            LogUtil.errorAnonymous(logger, "验证邀请码失败: " + e.getMessage(), request);
            return ApiResponse.error(500, "验证邀请码失败: " + e.getMessage());
        }
    }
}