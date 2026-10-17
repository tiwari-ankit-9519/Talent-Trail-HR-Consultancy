package com.abchr.platform.shared.logging;

import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SqlLogConfig {

    @Bean
    static BeanPostProcessor sqlLoggingDataSourcePostProcessor(Environment environment) {
        boolean enabled = environment.getProperty("app.logging.sql.enabled", Boolean.class, true);
        boolean params = environment.getProperty("app.logging.sql.params", Boolean.class, true);
        long slowMs = environment.getProperty("app.logging.sql.slow-ms", Long.class, 500L);
        int maxValue = environment.getProperty("app.logging.sql.max-value-length", Integer.class, 120);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!enabled || !(bean instanceof DataSource dataSource) || bean instanceof ProxyDataSource) {
                    return bean;
                }
                return ProxyDataSourceBuilder.create(dataSource)
                        .name("sql")
                        .listener(new SqlLogListener(params, slowMs, maxValue))
                        .build();
            }
        };
    }
}
