package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionSubmitResponse;

import java.math.BigDecimal;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 作业提交控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WorkSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkSubmissionService workSubmissionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ==================== 正常数据测试 ====================

    /**
     * 测试提交作业 - 成功
     */
    @Test
    @DisplayName("测试提交作业 - 成功")
    void testSubmitWork_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "homework.pdf",
                "application/pdf",
                "homework content".getBytes()
        );

        WorkSubmissionSubmitResponse response = new WorkSubmissionSubmitResponse();
        response.setId(1);

        Mockito.when(workSubmissionService.submitWork(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/submissions/")
                        .file(file)
                        .param("workId", "1")
                        .param("submissionContent", "这是我的作业")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询提交详情 - 成功
     */
    @Test
    @DisplayName("测试查询提交详情 - 成功")
    void testGetSubmissionDetail_Success() throws Exception {
        WorkSubmission submission = new WorkSubmission();
        submission.setId(1);
        submission.setSubmissionContent("作业内容");

        Mockito.when(workSubmissionService.getSubmissionById(Mockito.anyInt()))
                .thenReturn(submission);

        mockMvc.perform(get("/api/submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.submissionContent").value("作业内容"));
    }

    /**
     * 测试删除提交 - 成功
     */
    @Test
    @DisplayName("测试删除提交 - 成功")
    void testDeleteSubmission_Success() throws Exception {
        Mockito.doNothing().when(workSubmissionService).deleteSubmission(Mockito.anyInt());

        mockMvc.perform(delete("/api/submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询学生提交列表 - 成功
     */
    @Test
    @DisplayName("测试查询学生提交列表 - 成功")
    void testGetStudentSubmissions_Success() throws Exception {
        Mockito.when(workSubmissionService.getStudentSubmissions(Mockito.anyString(), Mockito.any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/submissions/student/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询作业的提交列表 - 成功
     */
    @Test
    @DisplayName("测试查询作业的提交列表 - 成功")
    void testGetWorkSubmissions_Success() throws Exception {
        Page<WorkSubmissionResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        Mockito.when(workSubmissionService.getWorkSubmissions(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/submissions/work/list")
                        .param("workId", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询已交名单 - 成功
     */
    @Test
    @DisplayName("测试查询已交名单 - 成功")
    void testGetSubmittedStudents_Success() throws Exception {
        Mockito.when(workSubmissionService.getSubmittedStudents(Mockito.anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/submissions/work/submitted")
                        .param("workId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询未交名单 - 成功
     */
    @Test
    @DisplayName("测试查询未交名单 - 成功")
    void testGetUnsubmittedStudents_Success() throws Exception {
        Mockito.when(workSubmissionService.getUnsubmittedStudents(Mockito.anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/submissions/work/unsubmitted")
                        .param("workId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试批改作业 - 成功
     */
    @Test
    @DisplayName("测试批改作业 - 成功")
    void testGradeWork_Success() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("95.0"));
        request.setComment("很好");

        WorkSubmission submission = new WorkSubmission();
        submission.setId(1);
        submission.setScore(new BigDecimal("95.0"));

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenReturn(submission);

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").value(95.0));
    }

    /**
     * 测试更新提交 - 成功
     */
    @Test
    @DisplayName("测试更新提交 - 成功")
    void testUpdateSubmission_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "updated.pdf",
                "application/pdf",
                "updated content".getBytes()
        );

        WorkSubmissionSubmitResponse response = new WorkSubmissionSubmitResponse();
        response.setId(1);

        Mockito.when(workSubmissionService.updateSubmission(
                Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/submissions/1")
                        .file(file)
                        .param("submissionContent", "更新后的内容")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 作业内容最大长度
     */
    @Test
    @DisplayName("边界测试 - 作业内容最大长度")
    void testSubmitWork_MaxContentLength() throws Exception {
        String longContent = "内".repeat(1000);

        WorkSubmissionSubmitResponse response = new WorkSubmissionSubmitResponse();
        response.setId(1);

        Mockito.when(workSubmissionService.submitWork(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", longContent)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 分数最大值100
     */
    @Test
    @DisplayName("边界测试 - 分数最大值100")
    void testGradeWork_MaxScore() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("100.0"));
        request.setComment("优秀");

        WorkSubmission submission = new WorkSubmission();
        submission.setId(1);
        submission.setScore(new BigDecimal("100.0"));

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenReturn(submission);

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 分数最小值0
     */
    @Test
    @DisplayName("边界测试 - 分数最小值0")
    void testGradeWork_MinScore() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("0.0"));
        request.setComment("需要改进");

        WorkSubmission submission = new WorkSubmission();
        submission.setId(1);
        submission.setScore(new BigDecimal("0.0"));

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenReturn(submission);

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 越界数据测试 ====================

    /**
     * 越界数据测试 - 作业内容超过最大长度
     */
    @Test
    @DisplayName("越界数据测试 - 作业内容超过最大长度")
    void testSubmitWork_ContentTooLong() throws Exception {
        String longContent = "内".repeat(2049);

        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", longContent)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 分数超过最大值
     */
    @Test
    @DisplayName("越界数据测试 - 分数超过最大值101")
    void testGradeWork_ScoreTooHigh() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("101.0"));

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.SCORE_OUT_OF_RANGE));

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 分数小于最小值
     */
    @Test
    @DisplayName("越界数据测试 - 分数小于最小值-1")
    void testGradeWork_ScoreTooLow() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("-1.0"));

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.SCORE_OUT_OF_RANGE));

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 分页大小超过最大限制
     */
    @Test
    @DisplayName("越界数据测试 - 分页大小超过最大限制301")
    void testGetWorkSubmissions_PageSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/submissions/work/list")
                        .param("workId", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "301"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 缺少 workId
     */
    @Test
    @DisplayName("非法数据测试 - 缺少 workId")
    void testSubmitWork_MissingWorkId() throws Exception {
        mockMvc.perform(multipart("/api/submissions/")
                        .param("submissionContent", "作业内容")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少必需的请求参数"));
    }

    /**
     * 非法数据测试 - workId为负数
     */
    @Test
    @DisplayName("非法数据测试 - workId为负数")
    void testSubmitWork_NegativeWorkId() throws Exception {
        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "-1")
                        .param("submissionContent", "作业内容")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - submissionId为空
     */
    @Test
    @DisplayName("非法数据测试 - submissionId为空")
    void testGradeWork_EmptySubmissionId() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(null);
        request.setScore(new BigDecimal("90.0"));

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - submissionId为负数
     */
    @Test
    @DisplayName("非法数据测试 - submissionId为负数")
    void testGradeWork_NegativeSubmissionId() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(-1);
        request.setScore(new BigDecimal("90.0"));

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 提交内容包含XSS攻击代码
     */
    @Test
    @DisplayName("非法数据测试 - 提交内容包含XSS攻击代码")
    void testSubmitWork_ContentWithXss() throws Exception {
        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", "<script>alert('xss')</script>")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 提交内容包含换行符
     */
    @Test
    @DisplayName("非法数据测试 - 提交内容包含换行符")
    void testSubmitWork_ContentWithNewline() throws Exception {
        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", "第一行\n第二行")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 提交不存在
     */
    @Test
    @DisplayName("非法数据测试 - 查询不存在的提交")
    void testGetSubmissionDetail_NotFound() throws Exception {
        Mockito.when(workSubmissionService.getSubmissionById(Mockito.anyInt()))
                .thenThrow(new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND));

        mockMvc.perform(get("/api/submissions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("提交记录不存在"));
    }

    /**
     * 非法数据测试 - 作业不存在
     */
    @Test
    @DisplayName("非法数据测试 - 作业不存在")
    void testSubmitWork_WorkNotFound() throws Exception {
        Mockito.when(workSubmissionService.submitWork(Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.WORK_NOT_FOUND));

        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "999")
                        .param("submissionContent", "作业内容")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("作业不存在"));
    }

    /**
     * 非法数据测试 - 非班级成员提交作业
     */
    @Test
    @DisplayName("非法数据测试 - 非班级成员提交作业")
    void testSubmitWork_NotClassMember() throws Exception {
        Mockito.when(workSubmissionService.submitWork(Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.NOT_IN_CLASS));

        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", "作业内容")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("你不是该班级的成员"));
    }

    /**
     * 非法数据测试 - 作业已截止提交
     */
    @Test
    @DisplayName("非法数据测试 - 作业已截止提交")
    void testSubmitWork_WorkExpired() throws Exception {
        Mockito.when(workSubmissionService.submitWork(Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR));

        mockMvc.perform(multipart("/api/submissions/")
                        .param("workId", "1")
                        .param("submissionContent", "作业内容")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("作业状态错误"));
    }

    /**
     * 非法数据测试 - 没有权限批改作业
     */
    @Test
    @DisplayName("非法数据测试 - 没有权限批改作业")
    void testGradeWork_NoPermission() throws Exception {
        GradeWorkRequest request = new GradeWorkRequest();
        request.setSubmissionId(1);
        request.setScore(new BigDecimal("90.0"));
        request.setComment("评语");

        Mockito.when(workSubmissionService.gradeWork(Mockito.any(GradeWorkRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.PERMISSION_DENIED));

        mockMvc.perform(put("/api/submissions/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足"));
    }
}