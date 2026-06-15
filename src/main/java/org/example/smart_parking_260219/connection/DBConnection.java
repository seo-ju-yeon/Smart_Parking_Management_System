package org.example.smart_parking_260219.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DB 연결을 관리하는 클래스입니다.
 *
 * <p>
 *     HikariCP를 사용해서 MariaDB와 연결하고,
 *     DAO에서 DB 작업을 할 때 필요한 Connection 객체를 가져올 수 있도록 만들었습니다.
 * </p>
 *
 * <p>
 *     enum을 사용해서 프로젝트 안에서 하나의 DBConnection 객체만 사용하도록 했습니다.
 * </p>
 */
public enum DBConnection {
    // DBConnection의 싱글턴 인스턴스
    INSTANCE;

    // HikariCP 커넥션 풀 객체
    private final HikariDataSource dataSource;

    DBConnection() {
        HikariConfig config = new HikariConfig();

        // MariaDB 드라이버와 접속 정보를 설정
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/smart_parking_team2");
        config.setUsername("admin");
        config.setPassword("0219");

        // PrepareStatement를 재사용하기 위한 캐시 설정
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    /**
     * DB 작업에 사용할 Connection 객체를 반환합니다.
     *
     * @return 커넥션 풀에서 가져온 Connection 객체
     * @throws SQLException DB 연결을 가져오는 중 문제가 발생한 경우
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
