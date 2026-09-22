package top.thexiaola.dreamhwhub.module.school.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.thexiaola.dreamhwhub.module.school.dto.ApproveSchoolJoinRequest;
import top.thexiaola.dreamhwhub.module.school.dto.AssignSchoolAdminRequest;
import top.thexiaola.dreamhwhub.module.school.dto.CreateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.JoinSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.SetSchoolJoinApprovalRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberIdentityRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberRoleRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolDetailResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolJoinApplicationResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolMemberResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 学校管理服务接口
 * <p>
 * 学校由平台管理员创建并指派学校管理员；姓名与学工号是学校内身份，
 * 用户加入学校时填写，进入学校后不可自行修改，仅学校管理员可修改。
 */
public interface SchoolService {

    /**
     * 分页查询学校（平台管理员侧）
     *
     * @param keyword  学校名称关键字，可选
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 学校分页结果
     */
    Page<SchoolVO> listSchools(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 创建学校（平台管理员）
     *
     * @param request 创建请求
     * @return 创建完成的学校详情
     */
    SchoolDetailResponse createSchool(CreateSchoolRequest request);

    /**
     * 修改学校信息（平台管理员或学校管理员）
     *
     * @param schoolId 学校 ID
     * @param request  修改请求
     * @return 修改后的学校详情
     */
    SchoolDetailResponse updateSchool(Integer schoolId, UpdateSchoolRequest request);

    /**
     * 解散学校（平台管理员），学校下仍有班级时不允许解散
     *
     * @param schoolId 学校 ID
     */
    void dissolveSchool(Integer schoolId);

    /**
     * 指派或取消学校管理员（平台管理员）
     *
     * @param schoolId 学校 ID
     * @param request  指派请求
     * @return 目标用户的学校成员信息
     */
    SchoolMemberResponse assignSchoolAdmin(Integer schoolId, AssignSchoolAdminRequest request);

    /**
     * 查看学校详情（含当前用户在该校的身份与申请状态）
     *
     * @param schoolId 学校 ID
     * @return 学校详情
     */
    SchoolDetailResponse getSchoolDetail(Integer schoolId);

    /**
     * 查询当前用户加入的学校列表
     *
     * @return 学校详情列表
     */
    List<SchoolDetailResponse> getMySchools();

    /**
     * 加入学校：学校免审核时直接成为成员，否则提交待审核申请
     *
     * @param schoolId 学校 ID
     * @param request  加入请求（姓名与学工号）
     * @return 加入申请响应（免审核加入时状态为已通过）
     */
    SchoolJoinApplicationResponse joinSchool(Integer schoolId, JoinSchoolRequest request);

    /**
     * 查询当前用户在某学校的加入申请
     *
     * @param schoolId 学校 ID
     * @return 申请响应，无申请返回 null
     */
    SchoolJoinApplicationResponse getMyJoinApplication(Integer schoolId);

    /**
     * 分页查询加入学校的申请（学校管理员侧）
     *
     * @param schoolId 学校 ID
     * @param status   状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 申请分页结果
     */
    Page<SchoolJoinApplicationResponse> getJoinApplications(Integer schoolId, Integer status,
            Integer pageNum, Integer pageSize);

    /**
     * 审核加入学校申请（学校管理员）
     *
     * @param schoolId 学校 ID
     * @param request  审核请求
     */
    void approveJoinApplication(Integer schoolId, ApproveSchoolJoinRequest request);

    /**
     * 分页查询学校成员（学校管理员侧）
     *
     * @param schoolId 学校 ID
     * @param keyword  姓名/学工号/用户名关键字，可选
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 成员分页结果
     */
    Page<SchoolMemberResponse> getSchoolMembers(Integer schoolId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 设置学校成员角色（学校管理员），仅可在老师与学生之间调整
     *
     * @param schoolId 学校 ID
     * @param request  角色请求
     */
    void updateMemberRole(Integer schoolId, UpdateSchoolMemberRoleRequest request);

    /**
     * 修改学校成员的姓名与学工号（学校管理员）
     *
     * @param schoolId 学校 ID
     * @param request  身份请求
     */
    void updateMemberIdentity(Integer schoolId, UpdateSchoolMemberIdentityRequest request);

    /**
     * 将成员移出学校（学校管理员）
     * 成员仍在该校班级中时不允许移出，需先退出或踢出其所在班级
     *
     * @param schoolId 学校 ID
     * @param userId   目标成员的用户 ID
     */
    void removeMember(Integer schoolId, Integer userId);

    /**
     * 当前用户主动退出学校
     * 仍在该校班级中时不允许退出，需先退出相关班级
     *
     * @param schoolId 学校 ID
     */
    void leaveSchool(Integer schoolId);

    /**
     * 设置加入学校是否需要审核（学校管理员）
     *
     * @param schoolId 学校 ID
     * @param request  设置请求
     */
    void setJoinApprovalRequired(Integer schoolId, SetSchoolJoinApprovalRequest request);

    /**
     * 校验学校存在，不存在则抛出「学校不存在」业务异常
     *
     * @param schoolId 学校 ID
     */
    void requireSchoolExists(Integer schoolId);

    /**
     * 批量查询学校名称
     *
     * @param schoolIds 学校 ID 集合
     * @return 学校 ID 到名称的映射
     */
    Map<Integer, String> getSchoolNames(Collection<Integer> schoolIds);

    /**
     * 判断用户是否为学校老师（老师或学校管理员），老师才能创建班级
     *
     * @param schoolId 学校 ID
     * @param userId   用户 ID
     * @return true-是学校老师
     */
    boolean isSchoolTeacher(Integer schoolId, Integer userId);

    /**
     * 查询用户在学校的成员记录
     *
     * @param schoolId 学校 ID
     * @param userId   用户 ID
     * @return 成员记录，非成员返回 null
     */
    SchoolMember getMember(Integer schoolId, Integer userId);

    /**
     * 批量查询用户在指定学校的成员记录，供班级成员列表读取姓名与学工号
     *
     * @param schoolId 学校 ID
     * @param userIds  用户 ID 集合
     * @return 用户 ID 到成员记录的映射
     */
    Map<Integer, SchoolMember> getMembersByUserIds(Integer schoolId, Collection<Integer> userIds);
}
