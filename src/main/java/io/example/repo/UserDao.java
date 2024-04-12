package io.example.repo;

import static io.example.config.ConnectionManager.getConnection;

import io.example.domain.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public void save(User user) {
        final String sql = "INSERT INTO USERS VALUES(?, ?, ?, ?)";

        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            preparedStatement.setString(1, user.getUserId());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getName());
            preparedStatement.setString(4, user.getEmail());

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public User findByUserId(String userId) {
        final String sql = "SELECT userId, password, name, email FROM USERS WHERE userId = ?";

        ResultSet resultSet = null;

        try (
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
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
            } catch (SQLException ex) {
                log.error("Resource Closed Failed");
            }
        }
        throw new RuntimeException("Execute Query failed");
    }
}
