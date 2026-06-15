package com.capstone.data;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

class RepositoryQueryArchitectureTest {

  @Test
  void modifyingQueriesMustNotUseSelect() throws Exception {
    List<String> violations = new ArrayList<>();
    for (Class<?> repository :
        List.of(
            UserRepositoryJPA.class,
            EmailVerificationCodeRepositoryJPA.class,
            AccountChangeRequestRepositoryJPA.class,
            RefreshTokenRepositoryJPA.class,
            UserVinRepositoryJPA.class,
            VinRepositoryJPA.class,
            CompletedMaintenanceRepositoryJPA.class,
            CompletedRecallRepositoryJPA.class)) {
      for (Method method : repository.getDeclaredMethods()) {
        Query query = method.getAnnotation(Query.class);
        if (query == null) {
          continue;
        }
        String jpql = query.value().strip().toLowerCase();
        if (method.isAnnotationPresent(Modifying.class) && jpql.startsWith("select")) {
          violations.add(
              repository.getSimpleName() + "#" + method.getName() + " uses @Modifying with SELECT");
        }
        if (method.getReturnType() == boolean.class
            && jpql.startsWith("select")
            && !jpql.contains("case when")
            && !jpql.contains("count(")) {
          violations.add(
              repository.getSimpleName()
                  + "#"
                  + method.getName()
                  + " returns boolean but SELECT may yield null");
        }
      }
    }
    if (!violations.isEmpty()) {
      fail(String.join("\n", violations));
    }
  }
}
