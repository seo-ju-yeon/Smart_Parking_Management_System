package org.example.smart_parking_260219.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.smart_parking_260219.util.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// 설정된 DB에 직접 연결하므로 통합 테스트로 분류함
@Tag("integration")
public class ConnectionTest {

    @Test
    public void connectionTest() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");

            String url = AppConfig.get("db.url");
            String user = AppConfig.get("db.username");
            String pass = AppConfig.get("db.password");

            Connection connection = DriverManager.getConnection(url, user, pass);
            Assertions.assertNotNull(connection);
            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHikariCP() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl(AppConfig.get("db.url"));
        config.setUsername(AppConfig.get("db.username"));
        config.setPassword(AppConfig.get("db.password"));

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Connection success: " + connection);
            Assertions.assertNotNull(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
