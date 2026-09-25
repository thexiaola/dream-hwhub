package top.thexiaola.dreamhwhub.module.school.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.permission.annotation.RequirePermission;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.school.dto.AssignSchoolAdminRequest;
import top.thexiaola.dreamhwhub.module.school.dto.BatchApproveSchoolJoinRequest;
import top.thexiaola.dreamhwhub.module.school.dto.CreateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.school.vo.BatchReviewResult;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolDetailResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolJoinApplicationResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolMemberResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolVO;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.security.RequireSensitiveVerification;
import top.thexiaola.dreamhwhub.support.security.SensitiveOperations;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 学校管理控制器（平台管理员侧）
 * <p>
 * 学校由平台管理员创建，学校管理员由平台管理员指派，每个操作对应一个权限节点。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/schools")
@RequiredArgsConstructor
public class AdminSchoolController {

    private final SchoolService schoolService;

    /**
     * 分页查询学校（平台管理员）
     */
    @GetMapping
    @RequirePermission(PermissionNodes.SCHOOL_VIEW_ALL)
    public ApiResponse<Page<SchoolVO>> listSchools(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SchoolVO> schools = schoolService.listSchools(keyword, pageNum, pageSize);
        log.info("User {} queried {} schools for management, keyword={}",
                LogUtil.getUserInfo(currentUser), schools.getTotal(), keyword);
        return ApiResponse.success(schools);
    }

    /**
     * 分页查询全部学校的加入申请（平台管理员集中审核入口）
     *
     * 路径为字面量 applications，Spring 会优先于 /{schoolId} 匹配
     */
    @GetMapping("/applications")
    @RequirePermission(PermissionNodes.SCHOOL_VIEW_ALL)
    public ApiResponse<Page<SchoolJoinApplicationResponse>> listAllJoinApplications(
            @RequestParam(value = "schoolId", required = false) Integer schoolId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SchoolJoinApplicationResponse> applications = schoolService.listAllJoinApplications(
                schoolId, status, pageNum, pageSize);
        log.info("User {} queried {} school join applications for management, schoolId={}, status={}",
                LogUtil.getUserInfo(currentUser), applications.getTotal(), schoolId, status);
        return ApiResponse.success(applications);
    }

    /**
     * 批量审核加入申请（平台管理员，可跨学校）
     */
    @PutMapping("/applications/batch-approve")
    @RequirePermission(PermissionNodes.SCHOOL_UPDATE)
    public ApiResponse<BatchReviewResult> batchApproveAllJoinApplications(
            @Valid @RequestBody BatchApproveSchoolJoinRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        BatchReviewResult result = schoolService.batchApproveAllJoinApplications(request);
        log.info("User {} batch reviewed school join applications, handled: {}, skipped: {}",
                LogUtil.getUserInfo(currentUser), result.getHandled(), result.getSkipped());
        String message = result.getSkipped() > 0
                ? "已处理 " + result.getHandled() + " 条申请，跳过 " + result.getSkipped() + " 条"
                : "已处理 " + result.getHandled() + " 条申请";
        return ApiResponse.success(result, message);
    }

    /**
     * 查看学校详情（平台管理员）
     */
    @GetMapping("/{schoolId}")
    @RequirePermission(PermissionNodes.SCHOOL_VIEW_ALL)
    public ApiResponse<SchoolDetailResponse> getSchoolDetail(
            @PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolDetailResponse detail = schoolService.getSchoolDetail(schoolId);
        log.info("User {} queried school detail for management, id: {}",
                LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(detail);
    }

    /**
     * 创建学校（平台管理员）
     */
    @PostMapping
    @RequirePermission(PermissionNodes.SCHOOL_CREATE)
    public ApiResponse<SchoolDetailResponse> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolDetailResponse created = schoolService.createSchool(request);
        log.info("User {} created school {}", LogUtil.getUserInfo(currentUser), created.getSchoolName());
        return ApiResponse.success(created, "学校创建成功");
    }

    /**
     * 修改学校信息（平台管理员）
     */
    @PutMapping("/{schoolId}")
    @RequirePermission(PermissionNodes.SCHOOL_UPDATE)
    public ApiResponse<SchoolDetailResponse> updateSchool(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody UpdateSchoolRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolDetailResponse updated = schoolService.updateSchool(schoolId, request);
        log.info("User {} updated school {}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(updated, "学校信息更新成功");
    }

    /**
     * 解散学校（平台管理员），学校下仍有班级时不允许解散（需身份二次验证）
     */
    @DeleteMapping("/{schoolId}")
    @RequirePermission(PermissionNodes.SCHOOL_DISSOLVE)
    @RequireSensitiveVerification(value = "解散学校", key = SensitiveOperations.SCHOOL_DISSOLVE)
    public ApiResponse<Void> dissolveSchool(@PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.dissolveSchool(schoolId);
        log.info("User {} dissolved school {}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(null, "学校已解散");
    }

    /**
     * 指派或取消学校管理员（平台管理员，需身份二次验证）
     */
    @PutMapping("/{schoolId}/admin")
    @RequirePermission(PermissionNodes.SCHOOL_ADMIN_ASSIGN)
    @RequireSensitiveVerification(value = "指派学校管理员", key = SensitiveOperations.SCHOOL_ASSIGN_ADMIN)
    public ApiResponse<SchoolMemberResponse> assignSchoolAdmin(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody AssignSchoolAdminRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolMemberResponse member = schoolService.assignSchoolAdmin(schoolId, request);
        log.info("User {} set school {} admin, target: {}, assigned: {}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getUserAccount(), request.getAssigned());
        return ApiResponse.success(member,
                Boolean.TRUE.equals(request.getAssigned()) ? "已指派为学校管理员" : "已取消学校管理员身份");
    }
}
