package top.thexiaola.dreamhwhub.module.login.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class ModifyUserController {
    @PostMapping("/modify/info")
    public String modifyUserInfo() {
        // TODO: 需要允许用户修改自己的 user_no、username、id_name、phone
        return null;
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
