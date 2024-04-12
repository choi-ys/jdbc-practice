취업TOOL 글자수세기/맞춤법
글자수세기/맞춤법사진크기조정연봉계산기실수령액 계산기퇴직금 계산기실업급여 계산기연차/휴가 계산기졸업/전역연도 계산기학점 변환기어학점수 변환기
글자수세기 / 맞춤법 검사 내용 입력
DBCP를 적용한 JDBC 프로그래밍
===
# 목표
JDBC와 DBCP를 활용한 간단한 CRUD 학습

# JDBC 관련 내용 학습
## JDBC : Java Database Connectivity
- 자바 어플리케이션에서 DB 프로그래밍을 할 수 있도록 도와주는 표준 인터페이스
- JDBC 인터페이스들을 구현한 구현체들은 JDBC Driver라고 하며, 각 데이터베이스 벤더사(Mysql, Oracle 등)들이 제공

## DBCP : Database Connection Pool
- 미리 생성한 일정량의 DB 커넥션을 풀에 저장해 두고 DB와의 통신이 필요할 때 풀에서 커넥션을 가져다 사용하는 기법
- Spring boot 2.0부터는 Default 커넥션 풀로 HikariCP 사용
- DBCP에는 HikariCp, Apache Commons DBCP, Tomcat JDBC Pool 등이 있음

### DBCP 사용 시 주의 사항
- 커넥션의 사용 주체는 WAS 쓰레드 이므로 커넥션 개수는 WAS 쓰레드 수와 함께 고려해야 함
- DB 접근을 필요로 하는 동시 접속자 수가 너무 많은 경우, Pool에 커넥션이 반납될 때까지 사용자가 대기해야 함
- 커넥션 수를 크게 설정하는 경우
- 많은 수의 커넥션을 생성하고 Pool에 저장하기 위해 메모리 소모가 큰 대신 동시 접속자 수가 많아지더라도 사용자 대기시간이 짧음
- 커넥션 수를 작게 설정하는 경우
- 커넥션을 생성하고 Pool에 저장하기 위한 메모리 소모가 상대적으로 적은 대신, 사용자 요청을 처리하는 과정에서 Pool에 유휴 커넥션이 없다면 사용자 대기 시간이 길어질 수 있음
- 따라서 WAS 쓰레드 수와 함께 적정 커넥션 풀 사이즈를 설정해야함

## Datasource
- 커넥션 획득을 위한 표준 인터페이스
- HikariCP의 Datasource 사용

---

# 실습
## 실습 단계
- Step0. 실습 Project 생성 및 필요 의존성 추가
- Step1. DriverManager를 이용하여 매 요청 마다 커넥션을 생성하여 처리하는 회원 CRUD 구현
- Step2. Spring JDBC의 HikariCP를 이용하여 커넥션 풀에 생성된 커넥션을 이용한 회원 CRUD 구현
- Step3. JdbcTemplate을 이용한 코드 가독성 개선 리펙토링

### Step1 : DriverManager를 이용하여 매 요청 마다 커넥션 생성 후 처리하는 회원 저장 및 조회 구현
#### Step1 구현 내용
1. User 객체를 DB 에 저장/조회 하기 위해 DB와의 통신을 담당하는 UserDao를 `TDD 방식`으로 구현
    - `test/resources/db_schema.sql` : H2 In-memory DB 환경에서 TC 구동을 위해 필요한 `Table 생성 SQL script` 작성
```mysql-sql
DROP TABLE IF EXISTS USERS;
    
CREATE TABLE USERS (
    userId          varchar(12)		NOT NULL,
    password		varchar(12)		NOT NULL,
    name			varchar(20)		NOT NULL,
    email			varchar(50),
    
    PRIMARY KEY               (userId)
);
```
2. TC 수행 시 `test/resources/db_schema.sql`에 위치한 Script 실행을 위한 setUp 메소드 작성
    - `UserDaoTest.java` : TC 수행 전 ClassPath의 Resource(DDL Script) 실행을 위한 ResourceDatabasePopular 객체 설정
    - `ConnectionManager.java` : Script 실행을 위해 대상 DB(H2 In-memory)와 통신하기 위해 필요한 DataSource(Hikari) 객체 생성
```Java
public class UserDaoTest {
    @BeforeEach
    void setUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db_schema.sql"));
        DatabasePopulatorUtils.execute(populator, ConnectionManager.getDataSource());
    }
}
```
```Java
public class ConnectionManager {
    public static DataSource getDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName("org.h2.Driver");
        hikariDataSource.setJdbcUrl("jdbc:h2:mem://localhost/~/jdbc-practice;MODE=MySQL;DB_CLOSE_DELAY=-1");
        hikariDataSource.setUsername("sa");
        hikariDataSource.setPassword("");

        return hikariDataSource;
    }
}
```
3. User객체의 DB 저장을 위한 UserDAO의 create() 메서드 TC 구현
    - `User.java` : 저장 대상 User 객체 설계 및 구현
    - `UserDao.java` : User 객체 저장을 위해 DB로 Insert 구문을 요청할 UserDao의 create 메서드 구현
        - `DriverManager`를 이용한 커넥션 생성 부 구현
        - DB에 실행 SQL 전달하기 위해 Connection객체로 부터 PrepareStatement 객체 획득
        - PrepareStatement를 이용한 User Insert 쿼리 파라미터 설정 및 쿼리 실행
        - PrepareStatement를 이용하여 User Select 쿼리 파라미터 설정 및 쿼리 실행
            - User Select 쿼리 실행 후, ResultSet에 반환된 User column을 User 객체로 변환
        - finally 구문에서 DB 통신을 위해 사용한 자원(Connection, PrepareStatement, ResultSet) 반납 처리 구현
```java
public class UserDaoTest {
    @BeforeEach
    void setUp() {...}
    
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
```
```java
public class User {
    private String userId;
    private String password;
    private String name;
    private String email;

    private User(String userId, String password, String name, String email) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    public static User of(String userId, String password, String name, String email) {
        return new User(userId, password, name, email);
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(userId, user.userId) && Objects.equals(password, user.password) && Objects.equals(name,
            user.name) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, password, name, email);
    }
}
```
```java
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
```
---
### Step2 : Spring JDBC의 HikariCP를 이용하여 커넥션 풀에 생성된 커넥션을 이용한 회원 CRUD 구현
#### Step2의 구현 내용
- ConnectionManager.java : DriverManager를 통해 Connection을 생성하는 부분을 UserDao로부터 분리 (관심사 분리)
   - DriverManager를 통해 Connection을 생성하는 부분을 HikariCP에 미리 생성된 커넥션을 획득하는 부분으로 변경
   - 커넥션 풀이 미리 생성될 커넥션 수 설정을 위한 Maximum pool size 설정 추가
   - 커넥션 풀을 하나만 가지도록 static 영역에 DataSource 객체를 선언한 후, static 영역에서 초기화
   - DBCP 관련 설정 하드 코딩 상수화
- Closeable, AutoCloseable 구현체인 Connection, PrepareStatement 자원 자동 반납을 위한 try~resource 구문 적용
  - try~resource문 적용으로 인해 자원 반납 코드 누락으로 인한 메모리 누수 방지 및 비지니스 코드의 가독성 개선
```java
public class ConnectionManager {
    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_URL = "jdbc:h2:mem://localhost/~/jdbc-practice;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PW = "";
    private static final int MAX_POOL_SIZE = 40;

    private static final DataSource dataSource;

    static {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(DB_DRIVER);
        hikariDataSource.setJdbcUrl(DB_URL);
        hikariDataSource.setUsername(DB_USERNAME);
        hikariDataSource.setPassword(DB_PW);
        hikariDataSource.setMaximumPoolSize(MAX_POOL_SIZE);

        dataSource = hikariDataSource;
    }

    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```
```java
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
```
