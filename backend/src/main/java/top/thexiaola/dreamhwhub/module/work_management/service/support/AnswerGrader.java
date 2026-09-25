package top.thexiaola.dreamhwhub.module.work_management.service.support;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.module.work_management.constant.QuestionType;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkQuestion;

import java.math.BigDecimal;
import java.util.*;

/**
 * 客观题自动评判器
 * <p>
 * 依据题型比较学生作答与参考答案，返回是否答对。仅处理客观题
 * （单选/多选/判断/填空）；主观题与附加题返回「无法自动评判」。
 */
@Component
@Slf4j
public class AnswerGrader {

    /**
     * 自动评判结果
     *
     * @param correct  是否答对（null 表示无法自动评判）
     * @param gradable 是否可自动评判
     */
    public record Result(Boolean correct, boolean gradable) {
    }

    /**
     * 判断某题是否可自动评判
     */
    public boolean canAutoGrade(WorkQuestion question) {
        return question != null && QuestionType.isAutoGradable(question.getQuestionType());
    }

    /**
     * 自动评判单题
     *
     * @param question      题目（含参考答案）
     * @param studentAnswer 学生作答（原始 JSON 值）
     * @return 评判结果
     */
    public Result grade(WorkQuestion question, Object studentAnswer) {
        if (question == null || !canAutoGrade(question)) {
            return new Result(null, false);
        }
        String type = question.getQuestionType();
        String correctRaw = question.getCorrectAnswer();

        try {
            return switch (type) {
                case QuestionType.SINGLE, QuestionType.JUDGE ->
                        new Result(judgeChoice(correctRaw, studentAnswer, type), true);
                case QuestionType.MULTIPLE ->
                        new Result(judgeMultiple(correctRaw, studentAnswer), true);
                case QuestionType.FILL ->
                        new Result(judgeFill(correctRaw, studentAnswer), true);
                default -> new Result(null, false);
            };
        } catch (Exception e) {
            log.warn("Auto grade failed for question {}: {}", question.getId(), e.getMessage());
            return new Result(false, true);
        }
    }

    /** 单选/判断：规范化后等值比较（选择题比较选项键，判断题归一到 true/false） */
    private boolean judgeChoice(String correctRaw, Object studentAnswer, String type) {
        String expected = normalizeScalar(correctRaw, type);
        String actual = normalizeScalar(studentAnswer, type);
        return expected != null && expected.equals(actual);
    }

    /** 多选：集合相等（顺序无关，忽略重复） */
    private boolean judgeMultiple(String correctRaw, Object studentAnswer) {
        Set<String> expected = toKeySet(correctRaw);
        Set<String> actual = toKeySet(studentAnswer);
        return !expected.isEmpty() && expected.equals(actual);
    }

    /** 填空：学生答案命中任一可接受答案即正确（trim + 忽略大小写） */
    private boolean judgeFill(String correctRaw, Object studentAnswer) {
        String actual = asText(studentAnswer);
        if (actual == null) {
            return false;
        }
        Set<String> accepted = toTextSet(correctRaw);
        return accepted.stream().anyMatch(a -> a.equalsIgnoreCase(actual));
    }

    // ===== 规范化辅助 =====

    private String normalizeScalar(Object value, String type) {
        String text = asText(value);
        if (text == null) {
            return null;
        }
        if (QuestionType.JUDGE.equals(type)) {
            return normalizeJudge(text);
        }
        return text.trim().toUpperCase(Locale.ROOT);
    }

    /** 判断题答案归一：true/false */
    private String normalizeJudge(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.equals("true") || t.equals("t") || t.equals("1") || t.equals("对") || t.equals("√") || t.equals("yes")) {
            return "true";
        }
        if (t.equals("false") || t.equals("f") || t.equals("0") || t.equals("错") || t.equals("×") || t.equals("x") || t.equals("no")) {
            return "false";
        }
        return t;
    }

    /** 从原始值/JSON 文本取纯文本 */
    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            // 若字符串本身是 JSON（数组/对象）则解析后取首个有效文本
            if ((trimmed.startsWith("[") || trimmed.startsWith("{"))) {
                try {
                    Object parsed = JSONUtil.parse(trimmed);
                    return asText(parsed);
                } catch (Exception ignored) {
                    return trimmed;
                }
            }
            return trimmed;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof JSONArray array) {
            return array.isEmpty() ? null : asText(array.get(0));
        }
        return String.valueOf(value).trim();
    }

    /** 转为选项键集合（忽略大小写、去除空白） */
    private Set<String> toKeySet(Object value) {
        Set<String> keys = new LinkedHashSet<>();
        if (value == null) {
            return keys;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return keys;
            }
            if (trimmed.startsWith("[")) {
                try {
                    return toKeySet(JSONUtil.parseArray(trimmed));
                } catch (Exception ignored) {
                    // 退化为按分隔符拆分
                }
            }
            for (String part : trimmed.split("[,，\\s]+")) {
                if (!part.isBlank()) {
                    keys.add(part.trim().toUpperCase(Locale.ROOT));
                }
            }
            return keys;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                String text = asText(item);
                if (text != null) {
                    keys.add(text.toUpperCase(Locale.ROOT));
                }
            }
            return keys;
        }
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                String text = asText(item);
                if (text != null) {
                    keys.add(text.toUpperCase(Locale.ROOT));
                }
            }
        }
        return keys;
    }

    /** 转为可接受文本集合（填空题） */
    private Set<String> toTextSet(Object value) {
        Set<String> texts = new LinkedHashSet<>();
        if (value == null) {
            return texts;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return texts;
            }
            if (trimmed.startsWith("[")) {
                try {
                    return toTextSet(JSONUtil.parseArray(trimmed));
                } catch (Exception ignored) {
                    // 退化为单值
                }
            }
            texts.add(trimmed);
            return texts;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                String text = asText(item);
                if (text != null) {
                    texts.add(text);
                }
            }
            return texts;
        }
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                String text = asText(item);
                if (text != null) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    /**
     * 将任意答案值序列化为 JSON 字符串存储（null 存 null）
     */
    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return JSONUtil.toJsonStr(s);
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value.toString();
        }
        return JSONUtil.toJsonStr(value);
    }

    /**
     * 解析存储的答案 JSON 为原始对象（供前端展示）
     */
    public Object parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Object parsed = JSONUtil.parse(raw);
            if (parsed instanceof cn.hutool.json.JSONString) {
                return parsed.toString();
            }
            return parsed;
        } catch (Exception e) {
            return raw;
        }
    }

    /**
     * 计算得分：答对得满分，答错得 0
     */
    public BigDecimal scoreOf(WorkQuestion question, Boolean correct) {
        if (Boolean.TRUE.equals(correct) && question.getScore() != null) {
            return question.getScore();
        }
        return BigDecimal.ZERO;
    }
}
