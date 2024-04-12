package io.example.repo;

import io.example.domain.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    private static Connection getConnection() {
        String url = "jdbc:h2:mem://localhost/~/jdbc-practice;MODE=MySQL;DB_CLOSE_DELAY=-1";
        String id = "sa";
        String password = "";
        try {
            return DriverManager.getConnection(url, id, password);
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void save(User user) {
        final String sql = "INSERT INTO USERS VALUES(?, ?, ?, ?)";

        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1, user.getUserId());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getName());
            preparedStatement.setString(4, user.getEmail());

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                    log.info("preparedStatement closed");
                }

                if (connection != null) {
                    connection.close();
                    log.info("connection closed");
                }
            } catch (SQLException ex) {
                log.error("Resource Closed Failed");
            }
        }
    }

    public User findByUserId(String userId) {
        final String sql = "SELECT userId, password, name, email FROM USERS WHERE userId = ?";

        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return User.of(
                    resultSet.getString("userId"),
                    resultSet.getString("password"),
                    resultSet.getString("name"),
                    resultSet.getString("email")
                );
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                    log.info("ResultSet closed");
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                    log.info("PreparedStatement closed");
                }

                if (connection != null) {
                    connection.close();
                    log.info("Connection closed");
                }
            } catch (SQLException ex) {
                log.error("Resource Closed Failed");
            }
        }
        throw new RuntimeException("Execute Query failed");
    }
}
