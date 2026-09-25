package top.thexiaola.dreamhwhub.module.work_management.service.support;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 反作弊文本打乱器
 * <p>
 * 依据 {@link ExamFontRegistry} 的映射，把真实文本逐字符替换为「显示码点」。
 * 映射未覆盖的字符（如生僻字、表情）原样保留。
 * <p>
 * 注意：映射是逐码点的双射（真实码点 -> 显示码点），因此打乱后长度不变，
 * 且保留原字符的显示顺序——前端配合字体即可正确渲染。
 */
@Component
public class ExamContentScrambler {

    /**
     * 按种子打乱文本
     *
     * @param text    真实文本（可为 null）
     * @param mapping 真实码点 -> 显示码点
     * @return 打乱后的文本；text 为 null 时返回 null
     */
    public String scramble(String text, Map<Integer, Integer> mapping) {
        if (text == null || text.isEmpty() || mapping == null || mapping.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            int code = text.charAt(i);
            Integer display = mapping.get(code);
            if (display != null) {
                sb.appendCodePoint(display);
            } else {
                sb.append((char) code);
            }
        }
        return sb.toString();
    }
}
