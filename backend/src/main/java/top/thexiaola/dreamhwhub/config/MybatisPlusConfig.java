package top.thexiaola.dreamhwhub.config;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>显式声明 {@link IdentifierGenerator}，使用固定的 workerId/datacenterId 构造雪花 ID 生成器，
 * 以替代 MyBatis-Plus starter 自动装配的默认实现。默认实现会通过
 * {@code InetAddress} 解析本机主机名并读取网卡 MAC 来计算 workerId/datacenterId；
 * 当运行环境 {@code /etc/hosts} 未配置本机 hostname 时，该解析会触发 DNS 反向查询并超时，
 * 导致应用启动阻塞约 10 秒（日志中表现为
 * {@code Initialization Sequence Very Slow!} 警告）。</p>
 *
 * <p>本项目所有实体主键均为数据库自增（{@code IdType.AUTO}），实际不使用雪花 ID，
 * 此处仅作兜底，固定取值即可。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new DefaultIdentifierGenerator(1L, 1L);
    }
}
