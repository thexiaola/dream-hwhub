package top.thexiaola.dreamhwhub.module.work_management.constant;

/**
 * 作业/考试类型常量
 * <p>
 * 考试与作业共用 {@code work_info} 表与题目/作答/评分链路，仅以 {@code work_type} 区分；
 * 考试额外支持限时与反作弊配置。
 */
public final class WorkType {

    private WorkType() {
    }

    /** 作业 */
    public static final String HOMEWORK = "homework";
    /** 考试 */
    public static final String EXAM = "exam";

    /** 是否为合法类型 */
    public static boolean isValid(String type) {
        return HOMEWORK.equals(type) || EXAM.equals(type);
    }

    /** 是否为考试 */
    public static boolean isExam(String type) {
        return EXAM.equals(type);
    }
}
