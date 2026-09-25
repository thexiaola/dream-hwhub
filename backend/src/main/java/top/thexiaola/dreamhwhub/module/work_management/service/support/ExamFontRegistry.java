package top.thexiaola.dreamhwhub.module.work_management.service.support;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 反作弊字体映射注册表
 * <p>
 * 启动时加载 classpath 下预生成的打乱字体池（{@code exam-font/pool/<seed>.json} +
 * {@code <seed>.woff2}）。每个种子对应一张「真实码点 -> 显示码点」映射与一个字体文件：
 * <ul>
 *   <li>服务端用映射把题干打乱成「显示码点」，明文不出网；</li>
 *   <li>前端加载该字体后，显示码点被渲染成真实字形，学生看到的是正常题干；</li>
 *   <li>但 DOM 文本是乱码，复制粘贴出去无法直接用于搜题。</li>
 * </ul>
 */
@Slf4j
@Component
public class ExamFontRegistry {

    /** 字体池在 classpath 下的目录 */
    private static final String POOL_DIR = "exam-font/pool";

    /** seed -> (真实码点 -> 显示码点) */
    private final Map<Integer, Map<Integer, Integer>> seedToMapping = new TreeMap<>();

    @PostConstruct
    public void load() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + POOL_DIR + "/*.json");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".json")) {
                    continue;
                }
                int seed = Integer.parseInt(filename.substring(0, filename.length() - ".json".length()));
                try (InputStream in = resource.getInputStream()) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject obj = JSONUtil.parseObj(text);
                    JSONObject pairs = obj.getJSONObject("pairs");
                    Map<Integer, Integer> mapping = new HashMap<>(pairs.size() * 2);
                    for (String realCode : pairs.keySet()) {
                        mapping.put(Integer.parseInt(realCode), pairs.getInt(realCode));
                    }
                    seedToMapping.put(seed, mapping);
                }
            }
            log.info("ExamFontRegistry loaded {} font mappings, seeds: {}", seedToMapping.size(), seedToMapping.keySet());
        } catch (Exception e) {
            log.error("Failed to load exam font mappings: {}", e.getMessage(), e);
        }
    }

    /**
     * 是否已加载到可用的字体映射
     */
    public boolean isAvailable() {
        return !seedToMapping.isEmpty();
    }

    /**
     * 可选种子列表（按升序）
     */
    public List<Integer> seeds() {
        return new ArrayList<>(seedToMapping.keySet());
    }

    /**
     * 是否存在指定种子
     */
    public boolean hasSeed(Integer seed) {
        return seed != null && seedToMapping.containsKey(seed);
    }

    /**
     * 取某个种子的映射（真实码点 -> 显示码点）；种子不存在返回空映射
     */
    public Map<Integer, Integer> mapping(Integer seed) {
        return seedToMapping.getOrDefault(seed, Map.of());
    }
}
