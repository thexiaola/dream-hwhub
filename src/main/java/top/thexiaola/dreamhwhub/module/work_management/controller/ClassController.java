package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInviteApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.dto.*;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.UserUtils;

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
        ClassApplication application = classService.submitCreateClassRequest(
                request.getClassName(), request.getDescription());
        ClassApplicationResponse response = new ClassApplicationResponse(
                application.getId(),
                application.getType(),
                application.getClassId(),
                application.getApplicantId(),
                application.getTargetRole(),
                application.getClassName(),
                application.getDescription(),
                application.getStatus(),
                application.getCreateTime()
        );
        log.info("User {} submitted create class application, id: {}", userInfo, application.getId());
        return ApiResponse.success(response, "申请已提交，待审核");
    }

    /**
     * 提交加入班级申请
     */
    @PostMapping("/join")
    public ApiResponse<ClassApplicationResponse> applyJoinClass(@Valid @RequestBody JoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to join class by ID: {}", userInfo, request.getClassCode());
        int classId = Integer.parseInt(request.getClassCode());
        ClassApplication application = classService.submitJoinClassRequest(classId, request.getIsTeacher());
        String roleStr = request.getIsTeacher() ? "TEACHER" : "STUDENT";
        ClassApplicationResponse response = new ClassApplicationResponse(
                application.getId(),
                application.getType(),
                application.getClassId(),
                application.getApplicantId(),
                application.getTargetRole(),
                application.getClassName(),
                application.getDescription(),
                application.getStatus(),
                application.getCreateTime()
        );
        log.info("User {} submitted join class application, role: {}", userInfo, roleStr);
        return ApiResponse.success(response, "申请已提交，待审核");
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
    public ApiResponse<List<ClassDetailResponse>> getMyClasses() {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying my classes list", userInfo);
        if (currentUser == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        List<ClassDetailResponse> classes = classService.getMyClasses(currentUser.getId());
        log.info("User {} queried {} classes", userInfo, classes.size());
        return ApiResponse.success(classes);
    }

    /**
     * 获取班级成员列表
     */
    @GetMapping("/members")
    public ApiResponse<List<ClassMemberResponse>> getClassMembers(@RequestParam Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying class members, class ID: {}", userInfo, classId);
        List<ClassMemberResponse> members = classService.getClassMembers(classId);
        log.info("User {} queried {} members", userInfo, members.size());
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
            role = classService.isTeacher(classId, currentUser.getId()) ? "TEACHER" : "STUDENT";
        }
        MemberCheckResponse response = new MemberCheckResponse(isMember, role);
        log.info("User {} check result: isMember={}, role={}", userInfo, isMember, role);
        return ApiResponse.success(response);
    }

    /**
     * 获取所有待审核的申请列表（管理员专用）
     */
    @GetMapping("/applications/pending")
    public ApiResponse<List<ClassApplication>> getPendingApplications() {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} querying pending applications", userInfo);
        List<ClassApplication> applications = classService.getPendingApplications();
        log.info("User {} queried {} pending applications", userInfo, applications.size());
        return ApiResponse.success(applications);
    }

    /**
     * 邀请用户加入班级（通过账号）- 老师/管理员专用，直接加入
     */
    @PostMapping("/invite")
    public ApiResponse<ClassMember> inviteUser(@RequestParam Integer classId,
                                                @RequestParam String userAccount,
                                                @RequestParam Boolean isTeacher) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} inviting user {} to class {} as {}", 
                userInfo, userAccount, classId, isTeacher ? "TEACHER" : "STUDENT");
        ClassMember member = classService.inviteUserToClass(classId, userAccount, isTeacher);
        log.info("User {} invited user {} to class {} successfully", userInfo, userAccount, classId);
        return ApiResponse.success(member);
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
                                                                  @RequestParam String userAccount,
                                                                  @RequestParam Boolean isTeacher) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("Student {} inviting user {} to class {} (needs approval)", 
                userInfo, userAccount, classId);
        ClassInviteApplication application = classService.studentInviteUser(classId, userAccount, isTeacher);
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
                userInfo, request.getMemberId(), request.getApproved());
        classService.approveInviteApplication(Integer.parseInt(request.getMemberId()), request.getApproved(), request.getComment());
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
}