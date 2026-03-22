package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class ModifyUserController {
    private static final Logger log = LoggerFactory.getLogger(ModifyUserController.class);
    private final ModifyUserService modifyUserService;
    public ModifyUserController(ModifyUserService modifyUserService) {
        this.modifyUserService = modifyUserService;
    }

    @PostMapping("/modify/info")
    public ResponseEntity<Map<String, Object>> modifyUserInfo(HttpServletRequest request, @Valid @RequestBody ModifyUserInfoRequest modifyUserInfoRequest) {
        String ip = LogUtil.getCurrentClientIp();
        ServiceResult<User> result = modifyUserService.modifyUserInfo(modifyUserInfoRequest);
        if (result.isSuccess()) {
            User user = result.getData();
            UserResponse userResponse = UserResponse.fromEntity(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify user info successful", userInfo);
            Map<String, Object> response = createSuccessModifyUserInfoResponse(userResponse);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = createErrorModifyUserInfoResponse(result.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/modify/email")
    public String modifyUserEmail() {
        // TODO: 需要允许用户修改自己的 email
        return null;
    }

    @PostMapping("/modify/password")
    public String modifyUserPassword() {
        // TODO: 需要允许用户修改自己的密码
        return null;
    }

    /**
     * 修改用户信息成功响应
     */
    private Map<String, Object> createSuccessModifyUserInfoResponse(Object data) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 200);
        response.put("message", "修改用户信息成功！");
        response.put("data", data);
        return response;
    }

    /**
     * 修改用户信息错误响应
     */
    private Map<String, Object> createErrorModifyUserInfoResponse(String message) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 400);
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    /**
     * 创建基础响应Map
     */
    private Map<String, Object> createBaseResponseMap() {
        return new LinkedHashMap<>();
    }
}