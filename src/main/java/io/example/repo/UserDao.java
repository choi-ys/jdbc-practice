package io.example.repo;

import io.example.domain.User;
import io.example.template.JdbcTemplate;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public void save(User user) {
        final String sql = "INSERT INTO USERS VALUES(?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.executeUpdate(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, user.getUserId());
                preparedStatement.setString(2, user.getPassword());
                preparedStatement.setString(3, user.getName());
                preparedStatement.setString(4, user.getEmail());
            }
        });
    }

    public User findByUserId(String userId) {
        final String sql = "SELECT userId, password, name, email FROM USERS WHERE userId = ?";

        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        return jdbcTemplate.executeQuery(
            sql,
            preparedStatement -> preparedStatement.setString(1, userId),
            (resultSet, rowNum) -> User.of(
                resultSet.getString("userId"),
                resultSet.getString("password"),
                resultSet.getString("name"),
                resultSet.getString("email")
            )
        );
    }
}
