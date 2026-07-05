package org.service.b.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Ensures @Scheduled jobs annotated with @SchedulerLock run on only one
 * instance at a time (e.g. during a deploy where two instances briefly overlap).
 * The lock is held in the shared Postgres "shedlock" table.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // use DB clock, avoids clock skew between instances
                .build()
        );
    }
}
