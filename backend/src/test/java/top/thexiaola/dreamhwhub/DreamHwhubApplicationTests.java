package top.thexiaola.dreamhwhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主测试类 - 运行所有测试的入口
 */
@SpringBootTest(properties = {
        "logging.level.org.springframework=WARN",
        "logging.level.root=INFO"
})
public class DreamHwhubApplicationTests {
    @Test
    void contextLoads() {
    }
}
