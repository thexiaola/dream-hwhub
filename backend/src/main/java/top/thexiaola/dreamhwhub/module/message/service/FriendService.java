package top.thexiaola.dreamhwhub.module.message.service;

import top.thexiaola.dreamhwhub.module.message.vo.AddableUserInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendRequestInfo;

import java.util.List;

/**
 * 好友服务接口
 * <p>
 * 好友按学校隔离：同一用户在不同学校拥有各自的好友列表；
 * 只允许添加同一学校内的用户为好友。
 */
public interface FriendService {

    /**
     * 我的好友列表（某学校）
     *
     * @param schoolId 学校 ID
     * @return 好友列表
     */
    List<FriendInfo> listFriends(Integer schoolId);

    /**
     * 我收到的待处理好友申请（某学校）
     *
     * @param schoolId 学校 ID
     * @return 好友申请列表
     */
    List<FriendRequestInfo> listPendingRequests(Integer schoolId);

    /**
     * 搜索可添加的同校用户
     *
     * @param schoolId 学校 ID
     * @param keyword  用户名 / 姓名 / 学工号关键字，可选
     * @return 可添加用户列表（含与我的关系）
     */
    List<AddableUserInfo> searchAddableUsers(Integer schoolId, String keyword);

    /**
     * 发起好友申请
     *
     * @param schoolId     学校 ID
     * @param targetUserId 目标用户 ID
     */
    void requestFriend(Integer schoolId, Integer targetUserId);

    /**
     * 响应好友申请（同意 / 拒绝）
     *
     * @param relationId 好友关系 ID
     * @param accepted   是否同意
     */
    void respondRequest(Integer relationId, Boolean accepted);

    /**
     * 删除好友（解除已建立的好友关系）
     *
     * @param relationId 好友关系 ID
     */
    void removeFriend(Integer relationId);
}
