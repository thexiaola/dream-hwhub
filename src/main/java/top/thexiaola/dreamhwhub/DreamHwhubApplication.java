package top.thexiaola.dreamhwhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "top.thexiaola.dreamhwhub")
@MapperScan("top.thexiaola.dreamhwhub.module.login.mapper")
public class DreamHwhubApplication {
    public static void main(String[] args) {
        SpringApplication.run(DreamHwhubApplication.class, args);
    }
}