package io.example.repo;

import static org.assertj.core.api.Assertions.assertThat;

import io.example.config.ConnectionManager;
import io.example.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class UserDaoTest {
    @BeforeEach
    void setUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db_schema.sql"));
        DatabasePopulatorUtils.execute(populator, ConnectionManager.getDataSource());
    }

    @Test
    @DisplayName("회원 저장")
    void saveUser() {
        // Given
        final String userId = "choi-ys";
        final String password = "password";
        final String name = "name";
        final String email = "email";

        User given = User.of(userId, password, name, email);

        // When
        UserDao userDao = new UserDao();
        userDao.save(given);

        // Then
        User actual = userDao.findByUserId(userId);
        assertThat(actual).isEqualTo(given);
    }
}
