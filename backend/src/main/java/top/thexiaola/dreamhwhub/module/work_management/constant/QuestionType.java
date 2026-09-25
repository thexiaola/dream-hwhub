package top.thexiaola.dreamhwhub.module.work_management.constant;

import java.util.List;

/**
 * 作业题型常量
 * <p>
 * 客观题（single/multiple/judge/fill）可系统自动评判，也支持老师手动评判；
 * 主观题（subjective）与附加题（extra，属于主观题）仅支持老师手动评判。
 */
public final class QuestionType {

    private QuestionType() {
    }

    /** 单项选择题 */
    public static final String SINGLE = "single";
    /** 多项选择题 */
    public static final String MULTIPLE = "multiple";
    /** 判断题 */
    public static final String JUDGE = "judge";
    /** 填空题 */
    public static final String FILL = "fill";
    /** 主观题 */
    public static final String SUBJECTIVE = "subjective";
    /** 附加题（属于主观题） */
    public static final String EXTRA = "extra";

    /** 全部合法题型 */
    public static final List<String> ALL = List.of(SINGLE, MULTIPLE, JUDGE, FILL, SUBJECTIVE, EXTRA);

    /**
     * 是否为客观题（可自动评判）
     */
    public static boolean isObjective(String type) {
        return SINGLE.equals(type) || MULTIPLE.equals(type) || JUDGE.equals(type) || FILL.equals(type);
    }

    /**
     * 是否需要老师手动评分（主观题与附加题）
     */
    public static boolean isSubjective(String type) {
        return SUBJECTIVE.equals(type) || EXTRA.equals(type);
    }

    /**
     * 是否可自动评判
     */
    public static boolean isAutoGradable(String type) {
        return isObjective(type);
    }

    /**
     * 是否合法题型
     */
    public static boolean isValid(String type) {
        return type != null && ALL.contains(type);
    }

    /**
     * 题型中文名
     */
    public static String nameOf(String type) {
        if (type == null) {
            return "未知题型";
        }
        return switch (type) {
            case SINGLE -> "单选题";
            case MULTIPLE -> "多选题";
            case JUDGE -> "判断题";
            case FILL -> "填空题";
            case SUBJECTIVE -> "主观题";
            case EXTRA -> "附加题";
            default -> "未知题型";
        };
    }
}
