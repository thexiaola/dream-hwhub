package top.thexiaola.dreamhwhub.module.work_management.constant;

import java.util.List;

/**
 * 考试违规类型常量
 * <p>
 * 反作弊检测到违规时，按类型记录一条 {@code exam_violation}，并累加会话的违规次数。
 */
public final class ExamViolationType {

    private ExamViolationType() {
    }

    /** 退出全屏（强制全屏模式下） */
    public static final String FULLSCREEN_EXIT = "fullscreen_exit";
    /** 切出页面（标签页不可见） */
    public static final String VISIBILITY_HIDDEN = "visibility_hidden";
    /** 窗口失焦 */
    public static final String WINDOW_BLUR = "window_blur";
    /** 复制 */
    public static final String COPY = "copy";
    /** 剪切 */
    public static final String CUT = "cut";
    /** 粘贴 */
    public static final String PASTE = "paste";

    /** 全部合法违规类型 */
    public static final List<String> ALL = List.of(
            FULLSCREEN_EXIT, VISIBILITY_HIDDEN, WINDOW_BLUR, COPY, CUT, PASTE);

    /** 是否合法违规类型 */
    public static boolean isValid(String type) {
        return type != null && ALL.contains(type);
    }

    /** 违规类型中文名 */
    public static String nameOf(String type) {
        if (type == null) {
            return "未知违规";
        }
        return switch (type) {
            case FULLSCREEN_EXIT -> "退出全屏";
            case VISIBILITY_HIDDEN -> "切出页面";
            case WINDOW_BLUR -> "窗口失焦";
            case COPY -> "复制内容";
            case CUT -> "剪切内容";
            case PASTE -> "粘贴内容";
            default -> "未知违规";
        };
    }
}
