package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生单题作答项（随提交作业提交）
 */
@Data
public class AnswerItem {

    /**
     * 题目 ID
     */
    @NotNull(message = "题目 ID 不能为空")
    private Integer questionId;

    /**
     * 作答内容：
     * 单选/判断-字符串（选项键，如 "A"），多选-字符串数组，填空-字符串，主观/附加-文本
     */
    private Object answer;
}
