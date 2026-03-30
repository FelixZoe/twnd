package cc.felixzoe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 虚拟线程配置
 * Java 21 虚拟线程提供极低的线程创建开销
 * 适合 IO 密集型场景（数据库查询、Redis操作、HTTP调用）
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class VirtualThreadConfiguration {

    /**
     * 异步任务执行器 - 使用虚拟线程
     * 用于 @Async 注解的方法
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("初始化虚拟线程执行器 (taskExecutor)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 通用虚拟线程执行器
     * 可注入到需要手动提交任务的 Service 中
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
