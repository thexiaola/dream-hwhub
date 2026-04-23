package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询请求DTO
 */
@Data
public class PageRequest {

    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    /**
     * 每页大小，最大300
     */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 300, message = "每页大小不能超过300")
    private Integer pageSize = 20;
}
