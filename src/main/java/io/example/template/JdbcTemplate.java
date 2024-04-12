package io.example.template;

import static io.example.config.ConnectionManager.getConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

public class JdbcTemplate {
    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    public void executeUpdate(String sql, PreparedStatementSetter preparedStatementSetter) {
        try (
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {

            preparedStatementSetter.setValues(preparedStatement);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public <T> T executeQuery(String sql, PreparedStatementSetter preparedStatementSetter, RowMapper<T> rowMapper) {
        try (
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            preparedStatementSetter.setValues(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return rowMapper.mapRow(resultSet, resultSet.getRow());
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        throw new IllegalArgumentException();
    }
}
