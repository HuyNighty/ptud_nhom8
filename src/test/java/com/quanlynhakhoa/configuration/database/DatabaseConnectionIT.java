
package com.quanlynhakhoa.configuration.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "n08.desktop.enabled=false"
)
class DatabaseConnectionIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToN08Database() throws Exception {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT DB_NAME()"
             )) {

            assertTrue(result.next());
            assertEquals("N08Dental", result.getString(1));
        }
    }
}
