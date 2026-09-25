package top.thexiaola.dreamhwhub.module.message.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.message.dto.UpdateMessagePolicyRequest;
import top.thexiaola.dreamhwhub.module.message.service.MessagePolicyService;
import top.thexiaola.dreamhwhub.module.message.vo.MessagePolicyInfo;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 私信策略控制器
 * <p>
 * 全站默认（平台管理员）与学校覆盖（学校管理员）：
 * 向陌生用户发送私信的条数上限与重置小时数。
 */
@Slf4j
@RestController
@RequestMapping("/api/message-policy")
@RequiredArgsConstructor
public class MessagePolicyController {

    private final MessagePolicyService messagePolicyService;

    /**
     * 读取全站默认策略（平台管理员设置页）
     */
    @GetMapping
    public ApiResponse<MessagePolicyInfo> getGlobalPolicy() {
        User currentUser = UserUtils.getCurrentUser();
        MessagePolicyInfo policy = messagePolicyService.getGlobalPolicy();
        log.info("User {} queried global message policy", LogUtil.getUserInfo(currentUser));
        return ApiResponse.success(policy);
    }

    /**
     * 修改全站默认策略（仅平台管理员）
     */
    @PutMapping
    public ApiResponse<MessagePolicyInfo> updateGlobalPolicy(
            @Valid @RequestBody UpdateMessagePolicyRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        MessagePolicyInfo policy = messagePolicyService.updateGlobalPolicy(
                request.getStrangerLimit(), request.getResetHours());
        log.info("User {} updated global message policy: limit={}, resetHours={}",
                LogUtil.getUserInfo(currentUser), request.getStrangerLimit(), request.getResetHours());
        return ApiResponse.success(policy, "全站私信策略已更新");
    }

    /**
     * 读取某学校生效的策略（学校覆盖优先，否则继承全站默认）
     */
    @GetMapping("/school/{schoolId}")
    public ApiResponse<MessagePolicyInfo> getSchoolPolicy(
            @PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        MessagePolicyInfo policy = messagePolicyService.getSchoolPolicy(schoolId);
        log.info("User {} queried school {} message policy", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(policy);
    }

    /**
     * 设置某学校覆盖策略（学校管理员；传 null 表示清除覆盖、继承全站默认）
     */
    @PutMapping("/school/{schoolId}")
    public ApiResponse<MessagePolicyInfo> updateSchoolPolicy(
            @PathVariable(value = "schoolId") Integer schoolId,
            @RequestBody UpdateMessagePolicyRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        MessagePolicyInfo policy = messagePolicyService.updateSchoolPolicy(
                schoolId, request.getStrangerLimit(), request.getResetHours());
        log.info("User {} updated school {} message policy: limit={}, resetHours={}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getStrangerLimit(), request.getResetHours());
        return ApiResponse.success(policy, "本校私信策略已更新");
    }
}
