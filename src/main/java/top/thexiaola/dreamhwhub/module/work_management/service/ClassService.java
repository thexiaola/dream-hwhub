package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassMemberResponse;

import java.util.List;

/**
 * 班级管理服务接口
 */
public interface ClassService {

    /**
     * 创建班级
     */
    ClassInfo createClass(String className, String description);

    /**
     * 加入班级
     */
    ClassMember joinClass(Integer classId, Boolean isTeacher);

    /**
     * 退出班级
     */
    void leaveClass(Integer classId);

    /**
     * 删除班级（仅创建者）
     */
    void deleteClass(Integer classId);

    /**
     * 查询用户所在的班级列表
     */
    List<ClassInfo> getUserClasses(Integer userId);

    /**
     * 检查用户在指定班级是否是老师
     */
    boolean isTeacher(Integer classId, Integer userId);

    /**
     * 检查用户在指定班级是否是学生
     */
    boolean isStudent(Integer classId, Integer userId);

    /**
     * 检查用户是否在指定班级中
     */
    boolean isClassMember(Integer classId, Integer userId);

    /**
     * 通过班级邀请码加入班级
     */
    ClassMember joinClassByCode(String classCode, Boolean isTeacher);

    /**
     * 验证班级邀请码并返回班级信息
     */
    ClassInfo verifyClassCode(String classCode);

    /**
     * 获取班级信息
     */
    ClassInfo getClassById(Integer classId);

    /**
     * 获取班级详情（包含统计信息）
     */
    ClassDetailResponse getClassDetail(Integer classId);

    /**
     * 获取用户加入的所有班级
     */
    List<ClassDetailResponse> getMyClasses(Integer userId);

    /**
     * 获取班级成员列表
     */
    List<ClassMemberResponse> getClassMembers(Integer classId);

    /**
     * 更新成员角色
     */
    void updateMemberRole(Integer classId, Integer userId, Boolean isTeacher);
}
