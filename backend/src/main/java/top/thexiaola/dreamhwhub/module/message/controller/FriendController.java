package top.thexiaola.dreamhwhub.module.message.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.message.dto.AddFriendRequest;
import top.thexiaola.dreamhwhub.module.message.dto.RespondFriendRequest;
import top.thexiaola.dreamhwhub.module.message.service.FriendService;
import top.thexiaola.dreamhwhub.module.message.vo.AddableUserInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendRequestInfo;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 好友控制器
 * <p>
 * 好友按学校隔离：同一用户在不同学校拥有各自的好友列表，只允许添加同校用户。
 */
@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 我的好友列表（某学校）
     */
    @GetMapping
    public ApiResponse<List<FriendInfo>> listFriends(
            @RequestParam(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        List<FriendInfo> friends = friendService.listFriends(schoolId);
        log.info("User {} queried {} friends in school {}",
                LogUtil.getUserInfo(currentUser), friends.size(), schoolId);
        return ApiResponse.success(friends);
    }

    /**
     * 我收到的待处理好友申请（某学校）
     */
    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestInfo>> listPendingRequests(
            @RequestParam(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        List<FriendRequestInfo> requests = friendService.listPendingRequests(schoolId);
        log.info("User {} queried {} pending friend requests in school {}",
                LogUtil.getUserInfo(currentUser), requests.size(), schoolId);
        return ApiResponse.success(requests);
    }

    /**
     * 搜索可添加的同校用户
     */
    @GetMapping("/search")
    public ApiResponse<List<AddableUserInfo>> searchAddableUsers(
            @RequestParam(value = "schoolId") Integer schoolId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        User currentUser = UserUtils.getCurrentUser();
        List<AddableUserInfo> users = friendService.searchAddableUsers(schoolId, keyword);
        log.info("User {} searched addable users in school {}, keyword={}, {} results",
                LogUtil.getUserInfo(currentUser), schoolId, keyword, users.size());
        return ApiResponse.success(users);
    }

    /**
     * 发起好友申请
     */
    @PostMapping("/requests")
    public ApiResponse<Void> requestFriend(@Valid @RequestBody AddFriendRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        friendService.requestFriend(request.getSchoolId(), request.getTargetUserId());
        log.info("User {} requested friend {}", LogUtil.getUserInfo(currentUser), request.getTargetUserId());
        return ApiResponse.success(null, "好友申请已发送");
    }

    /**
     * 响应好友申请（同意 / 拒绝）
     */
    @PutMapping("/requests")
    public ApiResponse<Void> respondRequest(@Valid @RequestBody RespondFriendRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        friendService.respondRequest(request.getRelationId(), request.getAccepted());
        log.info("User {} responded friend request {}, accepted={}",
                LogUtil.getUserInfo(currentUser), request.getRelationId(), request.getAccepted());
        return ApiResponse.success(null, Boolean.TRUE.equals(request.getAccepted()) ? "已同意好友申请" : "已拒绝好友申请");
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{relationId}")
    public ApiResponse<Void> removeFriend(@PathVariable(value = "relationId") Integer relationId) {
        User currentUser = UserUtils.getCurrentUser();
        friendService.removeFriend(relationId);
        log.info("User {} removed friend relation {}", LogUtil.getUserInfo(currentUser), relationId);
        return ApiResponse.success(null, "已删除好友");
    }
}
