package top.thexiaola.dreamhwhub.dto;

public class UserQueryRequest {
    private Integer page = 1;       // 页码，默认第1页
    private Integer size = 30;      // 每页大小，默认30条
    private String keyword;         // 搜索关键词（可搜索学号、用户名、邮箱）
    private Short permission;       // 权限筛选
    private Boolean active;         // 激活状态筛选

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Short getPermission() {
        return permission;
    }

    public void setPermission(Short permission) {
        this.permission = permission;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}