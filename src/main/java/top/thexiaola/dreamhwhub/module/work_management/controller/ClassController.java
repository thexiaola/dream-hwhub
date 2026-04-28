package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.work_management.dto.ApproveJoinClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.JoinClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.PageRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassCreateApplication;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInvitation;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassJoinApplication;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.vo.*;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/api/class")
public class ClassController {
    private static final Logger log = LoggerFactory.getLogger(ClassController.class);
    private final ClassService classService;
    
    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * 提交创建班级申请
     */
    @PostMapping("/create")
    public ApiResponse<CreateClassApplicationResponse> applyCreateClass(@Valid @RequestBody CreateClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to create class: {}", userInfo, request.getClassName());
        CreateClassApplicationResponse response = classService.submitCreateClassRequest(
                request.getClassName(), request.getDescription());
        log.info("User {} submitted create class application, id: {}", userInfo, response.getId());
        return ApiResponse.success(response, "创建班级的申请已提交，待审核");
    }

    /**
     * 提交加入班级申请
     */
    @PostMapping("/join")
    public ApiResponse<JoinClassApplicationResponse> applyJoinClass(@Valid @RequestBody JoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to join class by ID: {}", userInfo, request.getClassId());
        JoinClassApplicationResponse response = classService.submitJoinClassRequest(request.getClassId());
        log.info("User {} submitted join class application, role: STUDENT", userInfo);
        return ApiResponse.success(response, "加入班级的申请已提交，待审核");
    }

    /**
     * 退出班级
     */
    @PostMapping("/leave")
    public ApiResponse<Void> leaveClass(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} requesting to leave class, ID: {}", userInfo, classId);
        String className = classService.leaveClass(classId);
        log.info("User {} left class successfully", userInfo);
        return ApiResponse.success(null, "已成功退出“" + className + "”班级");
    }

    /**
     * 解散班级（仅创建者）
     */
    @DeleteMapping("/dissolve")
    public ApiResponse<Void> dissolveClass(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} requesting to dissolve class, ID: {}", userInfo, classId);
        classService.dissolveClass(classId);
        log.info("User {} dissolved class successfully", userInfo);
        return ApiResponse.success(null);
    }

    /**
     * 获取班级详情
     */
    @GetMapping("/detail")
    public ApiResponse<ClassDetailResponse> getClassDetail(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying class detail, ID: {}", userInfo, classId);
        ClassDetailResponse detail = classService.getClassDetail(classId);
        log.info("User {} queried class detail successfully", userInfo);
        return ApiResponse.success(detail);
    }

    /**
     * 获取我加入的班级列表
     */
    @GetMapping("/mylist")
    public ApiResponse<Page<ClassDetailResponse>> getMyClasses(@Valid @ModelAttribute PageRequest pageRequest) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying my classes list, page={}, size={}", userInfo, pageRequest.getPageNum(), pageRequest.getPageSize());
        if (currentUser == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        Page<ClassDetailResponse> classes = classService.getMyClasses(currentUser.getId(), pageRequest.getPageNum(), pageRequest.getPageSize());
        log.info("User {} queried {} classes", userInfo, classes.getTotal());
        return ApiResponse.success(classes);
    }

    /**
     * 获取班级成员列表（分页）
     */
    @GetMapping("/members")
    public ApiResponse<Page<ClassMemberResponse>> getClassMembers(
            @RequestParam Integer classId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying class members, class ID: {}, page={}, size={}", userInfo, classId, pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<ClassMemberResponse> members = classService.getClassMembers(classId, pageRequest.getPageNum(), pageRequest.getPageSize());
        log.info("User {} queried {} members", userInfo, members.getTotal());
        return ApiResponse.success(members);
    }

    /**
     * 检查用户是否在指定班级中
     */
    @GetMapping("/checkmember")
    public ApiResponse<MemberCheckResponse> checkMember(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} checking member status, class ID: {}", userInfo, classId);
        if (currentUser == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        boolean isMember = classService.isClassMember(classId, currentUser.getId());
        Integer roleCode = classService.getUserRoleCodeInClass(classId, currentUser.getId());
        String roleName = classService.getUserRoleNameInClass(classId, currentUser.getId());
        
        MemberCheckResponse response = new MemberCheckResponse(isMember, roleCode, roleName);
        log.info("User {} check result: isMember={}, roleCode={}, roleName={}", userInfo, isMember, roleCode, roleName);
        return ApiResponse.success(response);
    }

    /**
     * 获取创建班级申请列表（管理员专用，分页）
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     */
    @GetMapping("/applications/create/list")
    public ApiResponse<Page<ClassCreateApplication>> getCreateApplications(
            @RequestParam(required = false) Integer status,
            @Valid @ModelAttribute PageRequest pageRequest) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying create class applications with filter: status={}, page={}, size={}", 
                userInfo, status, pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<ClassCreateApplication> applications = classService.getCreateApplications(status, pageRequest.getPageNum(), pageRequest.getPageSize());
        log.info("User {} queried {} create class applications (total: {})", 
                userInfo, applications.getRecords().size(), applications.getTotal());
        return ApiResponse.success(applications, "查询创建申请列表成功");
    }

    /**
     * 审核创建班级申请（管理员专用）
     */
    @PutMapping("/applications/create/approve")
    public ApiResponse<Void> approveCreateApplication(@Valid @RequestBody ApproveJoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} approving create class application, id: {}, approved: {}",
                userInfo, request.getApplicationId(), request.getApproved());
        classService.approveCreateApplication(request.getApplicationId(), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} create class application {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 获取加入班级申请列表（老师和管理员专用，分页）
     * @param classId 班级 ID 筛选，可选
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     */
    @GetMapping("/applications/join/list")
    public ApiResponse<Page<ClassJoinApplication>> getJoinApplications(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer status,
            @Valid @ModelAttribute PageRequest pageRequest) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying join class applications with filters: classId={}, status={}, page={}, size={}", 
                userInfo, classId, status, pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<ClassJoinApplication> applications = classService.getJoinApplications(classId, status, pageRequest.getPageNum(), pageRequest.getPageSize());
        log.info("User {} queried {} join class applications (total: {})", 
                userInfo, applications.getRecords().size(), applications.getTotal());
        return ApiResponse.success(applications, "查询加入申请列表成功");
    }

    /**
     * 审核加入班级申请（老师和管理员专用）
     */
    @PutMapping("/applications/join/approve")
    public ApiResponse<Void> approveJoinApplication(@Valid @RequestBody ApproveJoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} approving join class application, id: {}, approved: {}",
                userInfo, request.getApplicationId(), request.getApproved());
        classService.approveJoinApplication(request.getApplicationId(), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} join class application {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 设置学生为班级助理（老师专用）
     */
    @PutMapping("/set-assistant-teacher")
    public ApiResponse<Void> setAssistantTeacher(@RequestParam Integer classId,
                                                  @RequestParam Integer studentUserId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} setting student {} as assistant teacher in class {}", 
                userInfo, studentUserId, classId);
        classService.setStudentAsAssistantTeacher(classId, studentUserId);
        log.info("User {} set student {} as assistant teacher successfully", userInfo, studentUserId);
        return ApiResponse.success(null);
    }

    /**
     * 将学生踢出班级（老师/班级助理专用）
     */
    @DeleteMapping("/kick-student")
    public ApiResponse<Void> kickStudent(@RequestParam Integer classId,
                                            @RequestParam Integer studentUserId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} kicking student {} from class {}", 
                userInfo, studentUserId, classId);
        classService.kickStudentFromClass(classId, studentUserId);
        log.info("User {} kicked student {} from class {} successfully", 
                userInfo, studentUserId, classId);
        return ApiResponse.success(null);
    }

    /**
     * 取消班级助理权限（降级为学生，仅创建者可用）
     */
    @PutMapping("/demote-assistant-teacher")
    public ApiResponse<Void> demoteAssistantTeacher(@RequestParam Integer classId,
                                                     @RequestParam Integer teacherUserId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} demoting assistant teacher {} to student in class {}", 
                userInfo, teacherUserId, classId);
        classService.demoteAssistantTeacher(classId, teacherUserId);
        log.info("User {} demoted assistant teacher {} to student in class {} successfully", 
                userInfo, teacherUserId, classId);
        return ApiResponse.success(null);
    }

    /**
     * 学生邀请用户加入班级（需要用户确认和教师审核）
     */
    @PostMapping("/student/invite")
    public ApiResponse<Void> studentInviteUser(@RequestParam Integer classId,
                                                                  @RequestParam String userAccount) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("Student {} inviting user {} to class {} (needs user confirmation and teacher approval)", 
                userInfo, userAccount, classId);
        classService.studentInviteUser(classId, userAccount);
        log.info("Student {} invitation sent successfully, waiting for user confirmation", userInfo);
        return ApiResponse.success(null, "邀请已发送，待用户确认");
    }

    /**
     * 被邀请用户响应邀请（同意/拒绝）
     */
    @PutMapping("/respond-user-invitation")
    public ApiResponse<Void> respondUserInvitation(@RequestParam Integer invitationId,
                                                    @RequestParam Boolean accepted) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} responding to user invitation, id: {}, accepted: {}",
                userInfo, invitationId, accepted);
        classService.respondUserInvitation(invitationId, accepted);
        String result = accepted ? "accepted" : "rejected";
        log.info("User {} user invitation {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 教师或助理审核邀请申请
     */
    @PutMapping("/approve-teacher-approval")
    public ApiResponse<Void> approveTeacherApproval(@Valid @RequestBody ApproveJoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} approving teacher approval, id: {}, approved: {}",
                userInfo, request.getApplicationId(), request.getApproved());
        classService.approveTeacherApproval(request.getApplicationId(), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} teacher approval {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 获取待教师审核的邀请列表（班级老师/助理专用）
     */
    @GetMapping("/teacher-approvals/pending")
    public ApiResponse<List<TeacherApprovalResponse>> getPendingTeacherApprovals(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying pending teacher approvals for class {}", userInfo, classId);
        List<TeacherApprovalResponse> approvals = classService.getPendingTeacherApprovals(classId);
        log.info("User {} queried {} pending teacher approvals", userInfo, approvals.size());
        return ApiResponse.success(approvals);
    }

    /**
     * 教师邀请用户加入班级（需用户同意）
     */
    @PostMapping("/invite-with-approval")
    public ApiResponse<ClassInvitation> inviteUserWithApproval(@RequestParam Integer classId,
                                                                @RequestParam String userAccount) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} sending invitation to user {} for class {} (requires approval)", 
                userInfo, userAccount, classId);
        ClassInvitation invitation = classService.inviteUserToClassWithApproval(classId, userAccount);
        log.info("User {} sent invitation, id: {}", userInfo, invitation.getId());
        return ApiResponse.success(invitation, "邀请已发送，等待用户响应");
    }

    /**
     * 获取我收到的邀请列表
     * @param status 状态筛选（0-待处理，1-已同意，2-已拒绝，3-已过期），可选
     */
    @GetMapping("/my-invitations")
    public ApiResponse<List<InvitationResponse>> getMyInvitations(
            @RequestParam(required = false) Integer status) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying my invitations with filter: status={}", userInfo, status);
        if (currentUser == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        List<InvitationResponse> invitations = classService.getMyInvitations(currentUser.getId(), status);
        log.info("User {} queried {} invitations", userInfo, invitations.size());
        return ApiResponse.success(invitations);
    }

    /**
     * 响应邀请（同意/拒绝）
     */
    @PutMapping("/respond-invitation")
    public ApiResponse<Void> respondInvitation(@RequestParam Integer invitationId,
                                                @RequestParam Boolean accepted) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} responding to invitation, id: {}, accepted: {}",
                userInfo, invitationId, accepted);
        classService.respondInvitation(invitationId, accepted);
        String result = accepted ? "accepted" : "rejected";
        log.info("User {} invitation {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 生成或刷新班级邀请码
     */
    @PostMapping("/generate-invite-code")
    public ApiResponse<String> generateInviteCode(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} generating invite code for class {}", userInfo, classId);
        String inviteCode = classService.generateOrRefreshInviteCode(classId);
        log.info("User {} generated invite code successfully", userInfo);
        return ApiResponse.success(inviteCode, "邀请码生成成功");
    }

    /**
     * 通过邀请码加入班级
     */
    @PostMapping("/join-by-code")
    public ApiResponse<ClassJoinApplication> joinByInviteCode(@RequestParam String inviteCode) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} joining class by invite code: {}", userInfo, inviteCode);
        ClassJoinApplication application = classService.joinClassByInviteCode(inviteCode);
        log.info("User {} submitted join application via invite code, id: {}", userInfo, application.getId());
        return ApiResponse.success(application, "加入申请已提交，待审核");
    }

    /**
     * 转让班级所有权
     */
    @PutMapping("/transfer-ownership")
    public ApiResponse<Void> transferOwnership(@RequestParam Integer classId,
                                                @RequestParam Integer newOwnerId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} transferring ownership of class {} to user {}", 
                userInfo, classId, newOwnerId);
        classService.transferClassOwnership(classId, newOwnerId);
        log.info("User {} transferred ownership of class {} to user {} successfully", 
                userInfo, classId, newOwnerId);
        return ApiResponse.success(null, "班级所有权转让成功");
    }
}