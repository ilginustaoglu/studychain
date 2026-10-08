package com.studychain.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Render (ve benzeri) ortamlarda sağlanan {@code DATABASE_URL} değerini
 * ({@code postgres(ql)://...}) JDBC {@link DataSource}'a dönüştürür.
 */
@Configuration(proxyBeanMethods = false)
@Conditional(RenderDatabaseConfiguration.DatabaseUrlPresent.class)
public class RenderDatabaseConfiguration {

    static final class DatabaseUrlPresent implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(context.getEnvironment().getProperty("DATABASE_URL"));
        }
    }

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        String databaseUrl = environment.getRequiredProperty("DATABASE_URL");
        return createHikariDataSource(databaseUrl);
    }

    private static HikariDataSource createHikariDataSource(String databaseUrl) {
        HikariDataSource ds = new HikariDataSource();
        if (databaseUrl.startsWith("jdbc:")) {
            ds.setJdbcUrl(databaseUrl);
            return ds;
        }
        URI uri = URI.create(databaseUrl.replaceFirst("^postgres(ql)?:", "http:"));
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("DATABASE_URL host bilgisi içermiyor");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath();
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            throw new IllegalArgumentException("DATABASE_URL veritabanı adı (path) içermiyor");
        }
        String database = path.startsWith("/") ? path.substring(1) : path;
        StringBuilder jdbcUrl = new StringBuilder();
        jdbcUrl.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(database);
        if (StringUtils.hasText(uri.getQuery())) {
            jdbcUrl.append("?").append(uri.getQuery());
        }
        ds.setJdbcUrl(jdbcUrl.toString());

        String userInfo = uri.getUserInfo();
        if (StringUtils.hasText(userInfo)) {
            int colon = userInfo.indexOf(':');
            if (colon < 0) {
                ds.setUsername(urlDecode(userInfo));
            } else {
                ds.setUsername(urlDecode(userInfo.substring(0, colon)));
                ds.setPassword(urlDecode(userInfo.substring(colon + 1)));
            }
        }
        return ds;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
