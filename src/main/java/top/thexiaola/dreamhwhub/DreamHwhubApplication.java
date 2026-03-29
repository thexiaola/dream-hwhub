package top.thexiaola.dreamhwhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {
        "top.thexiaola.dreamhwhub.module.login.mapper",
        "top.thexiaola.dreamhwhub.module.work_management.mapper"
})
public class DreamHwhubApplication {
    static void main(String[] args) {
        SpringApplication.run(DreamHwhubApplication.class, args);
    }
}