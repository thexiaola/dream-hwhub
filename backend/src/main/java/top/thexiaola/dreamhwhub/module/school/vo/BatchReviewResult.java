package top.thexiaola.dreamhwhub.module.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量审核加入申请的结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchReviewResult {

    /**
     * 实际处理的申请数量
     */
    private Integer handled;

    /**
     * 被跳过的申请数量（已处理、不属于该校或单条不满足审核条件）
     */
    private Integer skipped;
}
