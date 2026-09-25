package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.Data;

/**
 * 保存考试答题草稿请求
 * <p>
 * {@code draft} 为逐题作答的 JSON 字符串（由前端序列化），后端原样存储，
 * 刷新或断线后回传给前端恢复作答。
 */
@Data
public class SaveExamDraftRequest {

    /**
     * 作答草稿 JSON 字符串
     */
    private String draft;
}
