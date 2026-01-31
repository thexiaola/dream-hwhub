package top.thexiaola.dreamhwhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "logging.level.org.springframework=WARN",
    "logging.level.root=INFO"
})
class DreamHwhubApplicationTests {

    @Test
    void contextLoads() {
    }

}
