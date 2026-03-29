package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo;
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
     * 创建班级
     */
    @PostMapping("/create")
    public ApiResponse<ClassInfo> createClass(@Valid @RequestBody CreateClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} creating class: {}", userInfo, request.getClassName());
        ClassInfo classInfo = classService.createClass(request.getClassName(), request.getDescription());
        log.info("User {} created class successfully, ID: {}", userInfo, classInfo.getId());
        return ApiResponse.success(classInfo);
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
     * 提交创建班级申请
     */
    @PostMapping("/applycreate")
    public ApiResponse<ClassApplication> applyCreateClass(@Valid @RequestBody CreateClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to create class: {}", userInfo, request.getClassName());
        ClassApplication application = classService.submitCreateClassRequest(
                request.getClassName(), request.getDescription());
        log.info("User {} submitted create class application, id: {}", userInfo, application.getId());
        return ApiResponse.success(application);
    }

    /**
     * 提交加入班级申请
     */
    @PostMapping("/applyjoin")
    public ApiResponse<ClassApplication> applyJoinClass(@Valid @RequestBody JoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} applying to join class by ID: {}", userInfo, request.getClassCode());
        int classId = Integer.parseInt(request.getClassCode());
        ClassApplication application = classService.submitJoinClassRequest(classId, request.getIsTeacher());
        String roleStr = request.getIsTeacher() ? "TEACHER" : "STUDENT";
        log.info("User {} submitted join class application, role: {}", userInfo, roleStr);
        return ApiResponse.success(application);
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
     * 审核申请（管理员专用）
     */
    @PutMapping("/applications/approve")
    public ApiResponse<Void> approveApplication(@Valid @RequestBody ApproveJoinClassRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User {} approving application, id: {}, approved: {}",
                userInfo, request.getMemberId(), request.getApproved());
        classService.approveApplication(request.getMemberId(), request.getApproved(), request.getComment());
        String result = request.getApproved() ? "approved" : "rejected";
        log.info("User {} application {}", userInfo, result);
        return ApiResponse.success(null);
    }
}
