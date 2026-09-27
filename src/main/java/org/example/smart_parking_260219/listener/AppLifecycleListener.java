package org.example.smart_parking_260219.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.example.smart_parking_260219.connection.DBConnection;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class AppLifecycleListener implements ServletContextListener {

    private DBConnection dbConnection;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // 애플리케이션 시작 시 커넥션 풀을 생성하고 참조를 보관함
        dbConnection = DBConnection.INSTANCE;
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // JDBC 드라이버를 해제하기 전에 커넥션 풀부터 종료함
        if (dbConnection != null) {
            dbConnection.close();
            dbConnection = null;
        }

        deregisterJdbcDrivers(event);
    }

    private void deregisterJdbcDrivers(ServletContextEvent event) {
        // 다른 웹 애플리케이션의 드라이버는 건드리지 않도록
        // 현재 애플리케이션을 로딩한 클래스 로더를 기준으로 구분함
        ClassLoader applicationClassLoader =
                AppLifecycleListener.class.getClassLoader();

        Enumeration<Driver> drivers = DriverManager.getDrivers();

        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();

            // 현재 웹 애플리케이션이 등록한 드라이버만 해제함
            if (driver.getClass().getClassLoader() != applicationClassLoader) {
                continue;
            }

            try {
                DriverManager.deregisterDriver(driver);

                event.getServletContext().log(
                        "JDBC 드라이버 등록 해제 완료: "
                                + driver.getClass().getName()
                );
            } catch (SQLException e) {
                event.getServletContext().log(
                        "JDBC 드라이버 등록 해제 실패: "
                                + driver.getClass().getName(),
                        e
                );
            }
        }
    }
}
