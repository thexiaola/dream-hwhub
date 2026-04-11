package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassCreateApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInvitation;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInviteApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassJoinApplication;
import top.thexiaola.dreamhwhub.module.work_management.dto.ApproveJoinClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.JoinClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.RespondInvitationRequest;
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
    public ApiResponse<ClassApplicationResponse> applyCreateClass(@Valid @RequestBody CreateClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to create class: {}", userInfo, request.getClassName());
        ClassCreateApplication application = classService.submitCreateClassRequest(
                request.getClassName(), request.getDescription());
        ClassApplicationResponse response = new ClassApplicationResponse(
                application.getId(),
                null,  // type字段已废弃
                null,  // classId在审核通过后才会有
                application.getApplicantId(),
                application.getClassName(),
                application.getDescription(),
                application.getStatus(),
                application.getCreateTime()
        );
        log.info("User {} submitted create class application, id: {}", userInfo, application.getId());
        return ApiResponse.success(response, "创建班级的申请已提交，待审核");
    }

    /**
     * 提交加入班级申请
     */
    @PostMapping("/join")
    public ApiResponse<ClassApplicationResponse> applyJoinClass(@Valid @RequestBody JoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to join class by ID: {}", userInfo, request.getClassId());
        int classId = Integer.parseInt(request.getClassId());
        ClassJoinApplication application = classService.submitJoinClassRequest(classId);
        ClassApplicationResponse response = new ClassApplicationResponse(
                application.getId(),
                null,  // type字段已废弃
                application.getClassId(),
                application.getApplicantId(),
                null,  // className
                null,  // description
                application.getStatus(),
                application.getCreateTime()
        );
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
        classService.leaveClass(classId);
        log.info("User {} left class successfully", userInfo);
        return ApiResponse.success(null);
    }

    /**
     * 删除班级（仅创建者）
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteClass(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} requesting to delete class, ID: {}", userInfo, classId);
        classService.deleteClass(classId);
        log.info("User {} deleted class successfully", userInfo);
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
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ClassDetailResponse>> getMyClasses(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying my classes list, page={}, size={}", userInfo, pageNum, pageSize);
        if (currentUser == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ClassDetailResponse> classes = classService.getMyClasses(currentUser.getId(), pageNum, pageSize);
        log.info("User {} queried {} classes", userInfo, classes.getTotal());
        return ApiResponse.success(classes);
    }

    /**
     * 获取班级成员列表（分页）
     */
    @GetMapping("/members")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ClassMemberResponse>> getClassMembers(
            @RequestParam Integer classId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying class members, class ID: {}, page={}, size={}", userInfo, classId, pageNum, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ClassMemberResponse> members = classService.getClassMembers(classId, pageNum, pageSize);
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
        String role = null;
        if (isMember) {
            // 获取班级信息以确定是否是创建者
            top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo classInfo = classService.getClassById(classId);
            if (classInfo != null && classInfo.getOwnerId().equals(currentUser.getId())) {
                role = "OWNER";
            } else if (classService.isTeacher(classId, currentUser.getId())) {
                role = "ASSISTANT";
            } else {
                role = "STUDENT";
            }
        }
        MemberCheckResponse response = new MemberCheckResponse(isMember, role);
        log.info("User {} check result: isMember={}, role={}", userInfo, isMember, role);
        return ApiResponse.success(response);
    }

    /**
     * 获取创建班级申请列表（管理员专用）
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     */
    @GetMapping("/applications/create/list")
    public ApiResponse<List<ClassCreateApplication>> getCreateApplications(
            @RequestParam(required = false) Integer status) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying create class applications with filter: status={}", userInfo, status);
        List<ClassCreateApplication> applications = classService.getCreateApplications(status);
        log.info("User {} queried {} create class applications", userInfo, applications.size());
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
        classService.approveCreateApplication(Integer.parseInt(request.getApplicationId()), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} create class application {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 获取加入班级申请列表（老师和管理员专用）
     * @param classId 班级 ID 筛选，可选
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     */
    @GetMapping("/applications/join/list")
    public ApiResponse<List<ClassJoinApplication>> getJoinApplications(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer status) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying join class applications with filters: classId={}, status={}", 
                userInfo, classId, status);
        List<ClassJoinApplication> applications = classService.getJoinApplications(classId, status);
        log.info("User {} queried {} join class applications", userInfo, applications.size());
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
        classService.approveJoinApplication(Integer.parseInt(request.getApplicationId()), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} join class application {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 设置学生为助理老师（老师专用）
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
     * 将学生踢出班级（老师/助理老师专用）
     */
    @DeleteMapping("/remove-student")
    public ApiResponse<Void> removeStudent(@RequestParam Integer classId,
                                            @RequestParam Integer studentUserId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} removing student {} from class {}", 
                userInfo, studentUserId, classId);
        classService.removeStudentFromClass(classId, studentUserId);
        log.info("User {} removed student {} from class {} successfully", 
                userInfo, studentUserId, classId);
        return ApiResponse.success(null);
    }

    /**
     * 取消助理老师权限（降级为学生，仅创建者可用）
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
     * 学生邀请用户加入班级（需要审核）
     */
    @PostMapping("/student/invite")
    public ApiResponse<ClassInviteApplication> studentInviteUser(@RequestParam Integer classId,
                                                                  @RequestParam String userAccount) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("Student {} inviting user {} to class {} (needs approval)", 
                userInfo, userAccount, classId);
        ClassInviteApplication application = classService.studentInviteUser(classId, userAccount);
        log.info("Student {} submitted invite application, id: {}", userInfo, application.getId());
        return ApiResponse.success(application);
    }

    /**
     * 审核邀请申请（老师/管理员专用）
     */
    @PutMapping("/invite/approve")
    public ApiResponse<Void> approveInviteApplication(@Valid @RequestBody ApproveJoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} approving invite application, id: {}, approved: {}",
                userInfo, request.getApplicationId(), request.getApproved());
        classService.approveInviteApplication(Integer.parseInt(request.getApplicationId()), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} invite application {}", userInfo, result);
        return ApiResponse.success(null);
    }

    /**
     * 获取待审核的邀请申请列表（班级老师专用）
     */
    @GetMapping("/invite/applications/pending")
    public ApiResponse<List<ClassInviteApplication>> getPendingInviteApplications(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying pending invite applications for class {}", userInfo, classId);
        List<ClassInviteApplication> applications = classService.getPendingInviteApplications(classId);
        log.info("User {} queried {} pending invite applications", userInfo, applications.size());
        return ApiResponse.success(applications);
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
    public ApiResponse<Void> respondInvitation(@Valid @RequestBody RespondInvitationRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} responding to invitation, id: {}, accepted: {}",
                userInfo, request.getInvitationId(), request.getAccepted());
        int invitationId = Integer.parseInt(request.getInvitationId());
        classService.respondInvitation(invitationId, request.getAccepted(), request.getComment());
        String result = request.getAccepted() ? "accepted" : "rejected";
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