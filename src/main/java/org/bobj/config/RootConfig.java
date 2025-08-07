package org.bobj.config;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bobj.user.config.OAuth2ClientConfig;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;



@Configuration
@PropertySource(
    value = {
        "classpath:/application.properties",
        "classpath:/application.env"
    },
    ignoreResourceNotFound = true
)
@MapperScan(basePackages = {
        "org.bobj.order.mapper",
        "org.bobj.share.mapper",
        "org.bobj.property.mapper",
        "org.bobj.trade.mapper",
        "org.bobj.point.mapper",
        "org.bobj.user.mapper",
        "org.bobj.funding.mapper",
        "org.bobj.payment.mapper",
        "org.bobj.notification.mapper",
        "org.bobj.device.mapper",
        "org.bobj.allocation.mapper",
})
@ComponentScan(basePackages = "org.bobj")
@EnableTransactionManagement
@Import({
        AppConfig.class,
        OAuth2ClientConfig.class,
        S3Config.class
})
@EnableScheduling

public class RootConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setLocations(
            new ClassPathResource("application.properties"),
            new ClassPathResource("application.env") // ← 이거만 바꾸면 끝
        );
        return configurer;
    }


    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Autowired
    private final ApplicationContext applicationContext;

    public RootConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // DataSource 설정 (MySQL + HikariCP)
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("hthHikariPool");
        return new HikariDataSource(config);
    }

    // SqlSessionFactory 설정
    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource());
        factory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mappers/**/*.xml")
        );
        return factory.getObject();
    }

    // 트랜잭션 매니저 설정
    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}

