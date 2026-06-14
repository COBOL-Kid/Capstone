package com.capstone.support;

public final class IntegrationTestProperties {

  public static final String JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

  private IntegrationTestProperties() {}

  public static String[] h2CreateDrop(String dbName) {
    return new String[] {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:"
          + dbName
          + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=false",
      "security.jwt.secret=" + JWT_SECRET,
      "security.jwt.expiration-minutes=15",
      "security.jwt.refresh-expiration-days=7",
      "security.cookies.secure=false",
      "mailjet.enabled=true",
      "mailjet.api-key-public=test-public",
      "mailjet.api-key-private=test-private",
      "mailjet.from-email=sender@example.com",
      "mailjet.from-name=Honest Car"
    };
  }

  public static String[] h2FlywaySeed(String dbName) {
    return new String[] {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:"
          + dbName
          + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:integration-test-db/migration",
      "spring.flyway.target=1",
      "security.jwt.secret=" + JWT_SECRET,
      "security.jwt.expiration-minutes=15",
      "security.jwt.refresh-expiration-days=7",
      "security.cookies.secure=false",
      "mailjet.enabled=false"
    };
  }
}
