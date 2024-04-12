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
