package top.thexiaola.dreamhwhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 测试基类
 * 提供通用的测试配置和工具方法
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return JSON 字符串
     * @throws Exception 转换异常
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 获取 JSON 媒体类型
     *
     * @return MediaType.APPLICATION_JSON
     */
    protected MediaType getJsonMediaType() {
        return MediaType.APPLICATION_JSON;
    }
}
