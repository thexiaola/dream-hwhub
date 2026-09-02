package top.thexiaola.dreamhwhub.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>1. 注册分页插件，使 {@code selectPage} 真正执行分页与 count 查询，
 * 否则所有分页接口的 {@code total/pages} 恒为 0、数据也不会被截断。</p>
 *
 * <p>2. 显式声明 {@link IdentifierGenerator}，使用固定的 workerId/datacenterId 构造雪花 ID 生成器，
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

    /**
     * MyBatis-Plus 分页插件（MySQL）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new DefaultIdentifierGenerator(1L, 1L);
    }
}
