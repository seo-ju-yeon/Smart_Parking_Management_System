package org.example.smart_parking_260219.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.smart_parking_260219.util.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DBConnectionTest {
    @Test
    public void connectionTest() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");

            String url = AppConfig.get("db.url");
            String user = AppConfig.get("db.username");
            String passwd = AppConfig.get("db.password");

            Connection connection = DriverManager.getConnection(url, user, passwd);
            // 변수가 null이 아닌지 확인 -> null이 아니면 객체를 참조하고 있음.
            Assertions.assertNotNull(connection);
            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void TestHikariCP() {
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
            System.out.println("Connection success : " + connection);
            // 변수가 null이 아닌지 확인 -> null이 아니면 객체를 참조하고 있음.
            Assertions.assertNotNull(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
