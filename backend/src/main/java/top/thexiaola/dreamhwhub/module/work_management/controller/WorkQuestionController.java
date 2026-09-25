package top.thexiaola.dreamhwhub.module.work_management.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkQuestionService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 作业题目控制器
 * <p>
 * 供学生取题作答：教师侧返回含参考答案的题目（用于编辑/评阅），
 * 学生侧严格不下发参考答案与解析。
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/works")
@RequiredArgsConstructor
public class WorkQuestionController {

    private final WorkQuestionService workQuestionService;
    private final WorkService workService;
    private final ClassService classService;

    /**
     * 查询某作业的题目（按调用者身份返回：教师含参考答案，学生不含）
     */
    @GetMapping(value = "/{workId}/questions")
    public ResponseEntity<ApiResponse<List<?>>> getWorkQuestions(@PathVariable(value = "workId") Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            // 复用作业查询做权限校验（仅本班成员或该班老师可查看）
            WorkResponse work = workService.getWorkById(workId);
            Integer classId = work.getClassId();

            if (classService.isTeacher(classId, currentUser.getId())) {
                log.info("User ({}) queried work questions (teacher), workId: {}", userInfo, workId);
                return ResponseEntity.ok(ApiResponse.success(workQuestionService.listForTeacher(workId)));
            }
            log.info("User ({}) queried work questions (student), workId: {}", userInfo, workId);
            return ResponseEntity.ok(ApiResponse.success(workQuestionService.listForStudent(workId)));
        } catch (BusinessException e) {
            log.warn("User query work questions failed: {}", e.getMessage());
            throw e;
        }
    }
}
