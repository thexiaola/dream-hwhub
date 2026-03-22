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
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;


@RestController
@RequestMapping("/api/users")
public class ModifyUserController {
    private static final Logger log = LoggerFactory.getLogger(ModifyUserController.class);
    private final ModifyUserService modifyUserService;
    public ModifyUserController(ModifyUserService modifyUserService) {
        this.modifyUserService = modifyUserService;
    }

    @PostMapping("/modify/info")
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserInfo(HttpServletRequest request, @Valid @RequestBody ModifyUserInfoRequest modifyUserInfoRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserInfo(modifyUserInfoRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify user info successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
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
}