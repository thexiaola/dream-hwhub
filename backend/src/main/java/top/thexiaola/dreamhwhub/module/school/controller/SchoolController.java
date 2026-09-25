package top.thexiaola.dreamhwhub.module.school.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.school.dto.ApproveSchoolJoinRequest;
import top.thexiaola.dreamhwhub.module.school.dto.BatchApproveSchoolJoinRequest;
import top.thexiaola.dreamhwhub.module.school.vo.BatchReviewResult;
import top.thexiaola.dreamhwhub.module.school.dto.JoinSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.SetClassTakeoverApprovalRequest;
import top.thexiaola.dreamhwhub.module.school.dto.SetSchoolJoinApprovalRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberIdentityRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberRoleRequest;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolDetailResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolJoinApplicationResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolMemberResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolVO;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.security.RequireSensitiveVerification;
import top.thexiaola.dreamhwhub.support.security.SensitiveOperations;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 学校控制器（用户侧与学校管理员侧）
 * <p>
 * 所有用户可浏览学校、提交加入申请；学校管理员可审核加入申请、管理成员与加入设置。
 */
@Slf4j
@RestController
@RequestMapping("/api/school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * 分页查询学校列表（用于浏览与选择加入）
     */
    @GetMapping("/list")
    public ApiResponse<Page<SchoolVO>> listSchools(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SchoolVO> schools = schoolService.listSchools(keyword, pageNum, pageSize);
        log.info("User {} queried {} schools, keyword={}, page={}, size={}",
                LogUtil.getUserInfo(currentUser), schools.getTotal(), keyword, pageNum, pageSize);
        return ApiResponse.success(schools);
    }

    /**
     * 获取我加入的学校列表（可按学校名称关键字搜索，筛选在数据库中完成）
     */
    @GetMapping("/mine")
    public ApiResponse<List<SchoolDetailResponse>> getMySchools(
            @RequestParam(value = "minRoleCode", required = false) Integer minRoleCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        User currentUser = UserUtils.getCurrentUser();
        List<SchoolDetailResponse> schools = schoolService.getMySchools(minRoleCode, keyword);
        log.info("User {} queried {} joined schools, keyword={}",
                LogUtil.getUserInfo(currentUser), schools.size(), keyword);
        return ApiResponse.success(schools);
    }

    /**
     * 获取学校详情（含当前用户在该校的身份与申请状态）
     */
    @GetMapping("/{schoolId}")
    public ApiResponse<SchoolDetailResponse> getSchoolDetail(
            @PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolDetailResponse detail = schoolService.getSchoolDetail(schoolId);
        log.info("User {} queried school detail, id: {}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(detail);
    }

    /**
     * 加入学校：学校免审核时直接成为成员，否则提交待审核申请
     */
    @PostMapping("/{schoolId}/join")
    public ApiResponse<SchoolJoinApplicationResponse> joinSchool(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody JoinSchoolRequest request) {
        if (schoolId == null || schoolId <= 0) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "学校ID无效", null);
        }
        User currentUser = UserUtils.getCurrentUser();
        SchoolJoinApplicationResponse response = schoolService.joinSchool(schoolId, request);
        log.info("User {} joined school {}, application status: {}",
                LogUtil.getUserInfo(currentUser), schoolId, response.getStatus());
        boolean joined = Integer.valueOf(1).equals(response.getStatus());
        return ApiResponse.success(response, joined ? "已加入学校" : "加入学校的申请已提交，待学校管理员审核");
    }

    /**
     * 查询我在某学校的加入申请
     */
    @GetMapping("/{schoolId}/my-application")
    public ApiResponse<SchoolJoinApplicationResponse> getMyJoinApplication(
            @PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        SchoolJoinApplicationResponse application = schoolService.getMyJoinApplication(schoolId);
        log.info("User {} queried own school application, school ID: {}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(application);
    }

    /**
     * 获取加入学校申请列表（学校管理员）
     */
    @GetMapping("/{schoolId}/applications")
    public ApiResponse<Page<SchoolJoinApplicationResponse>> getJoinApplications(
            @PathVariable(value = "schoolId") Integer schoolId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SchoolJoinApplicationResponse> applications = schoolService.getJoinApplications(
                schoolId, status, pageNum, pageSize);
        log.info("User {} queried {} school join applications, schoolId={}, status={}",
                LogUtil.getUserInfo(currentUser), applications.getTotal(), schoolId, status);
        return ApiResponse.success(applications);
    }

    /**
     * 审核加入学校申请（学校管理员）
     */
    @PutMapping("/{schoolId}/applications/approve")
    public ApiResponse<Void> approveJoinApplication(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody ApproveSchoolJoinRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.approveJoinApplication(schoolId, request);
        log.info("User {} reviewed school {} application {}, approved: {}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getApplicationId(), request.getApproved());
        return ApiResponse.success(null);
    }

    /**
     * 批量审核加入学校申请（学校管理员）
     */
    @PutMapping("/{schoolId}/applications/batch-approve")
    public ApiResponse<BatchReviewResult> batchApproveJoinApplications(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody BatchApproveSchoolJoinRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        BatchReviewResult result = schoolService.batchApproveJoinApplications(schoolId, request);
        int handled = result.getHandled();
        int skipped = result.getSkipped();
        log.info("User {} batch reviewed school {} applications, handled: {}, skipped: {}, approved: {}",
                LogUtil.getUserInfo(currentUser), schoolId, handled, skipped, request.getApproved());
        String message = skipped > 0
                ? "已处理 " + handled + " 条申请，跳过 " + skipped + " 条"
                : "已处理 " + handled + " 条申请";
        return ApiResponse.success(result, message);
    }

    /**
     * 获取学校成员列表（学校管理员）
     */
    @GetMapping("/{schoolId}/members")
    public ApiResponse<Page<SchoolMemberResponse>> getSchoolMembers(
            @PathVariable(value = "schoolId") Integer schoolId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SchoolMemberResponse> members = schoolService.getSchoolMembers(schoolId, keyword, pageNum, pageSize);
        log.info("User {} queried {} school members, schoolId={}, keyword={}",
                LogUtil.getUserInfo(currentUser), members.getTotal(), schoolId, keyword);
        return ApiResponse.success(members);
    }

    /**
     * 设置学校成员角色（学校管理员），仅可在老师与学生之间调整
     */
    @PutMapping("/{schoolId}/members/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody UpdateSchoolMemberRoleRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.updateMemberRole(schoolId, request);
        log.info("User {} set school {} member {} role to {}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getUserId(), request.getRole());
        return ApiResponse.success(null, "成员角色已更新");
    }

    /**
     * 修改学校成员的姓名与学工号（学校管理员）
     */
    @PutMapping("/{schoolId}/members/identity")
    public ApiResponse<Void> updateMemberIdentity(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody UpdateSchoolMemberIdentityRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.updateMemberIdentity(schoolId, request);
        log.info("User {} updated school {} member {} identity",
                LogUtil.getUserInfo(currentUser), schoolId, request.getUserId());
        return ApiResponse.success(null, "成员姓名与学工号已更新");
    }

    /**
     * 将成员移出学校（学校管理员）
     */
    @DeleteMapping("/{schoolId}/members/{userId}")
    public ApiResponse<Void> removeMember(
            @PathVariable(value = "schoolId") Integer schoolId,
            @PathVariable(value = "userId") Integer userId) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.removeMember(schoolId, userId);
        log.info("User {} removed member {} from school {}",
                LogUtil.getUserInfo(currentUser), userId, schoolId);
        return ApiResponse.success(null, "成员已移出学校");
    }

    /**
     * 退出学校（需身份二次验证）
     */
    @DeleteMapping("/{schoolId}/membership")
    @RequireSensitiveVerification(value = "退出学校", key = SensitiveOperations.SCHOOL_LEAVE)
    public ApiResponse<Void> leaveSchool(@PathVariable(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.leaveSchool(schoolId);
        log.info("User {} left school {}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(null, "已退出学校");
    }

    /**
     * 设置加入学校是否需要审核（学校管理员）
     */
    @PutMapping("/{schoolId}/join-approval")
    public ApiResponse<Void> setJoinApprovalRequired(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody SetSchoolJoinApprovalRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.setJoinApprovalRequired(schoolId, request);
        log.info("User {} set school {} join approval, allowJoinWithoutApproval: {}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getAllowJoinWithoutApproval());
        return ApiResponse.success(null, "加入学校设置已更新");
    }

    /**
     * 设置班级接管是否自动同意（学校管理员）
     */
    @PutMapping("/{schoolId}/class-takeover-approval")
    public ApiResponse<Void> setClassTakeoverApproval(
            @PathVariable(value = "schoolId") Integer schoolId,
            @Valid @RequestBody SetClassTakeoverApprovalRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        schoolService.setClassTakeoverAutoApprove(schoolId, request);
        log.info("User {} set school {} class takeover auto approve: {}",
                LogUtil.getUserInfo(currentUser), schoolId, request.getAutoApproveClassTakeover());
        return ApiResponse.success(null, "班级接管设置已更新");
    }
}
