package com.capstone.data;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

class RepositoryQueryArchitectureTest {

  @Test
  void modifyingQueriesMustNotUseSelect() throws Exception {
    List<String> violations = new ArrayList<>();
    for (Class<?> repository : repositoryClasses()) {
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

  private static List<Class<?>> repositoryClasses() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*RepositoryJPA")));
    Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.capstone.data");
    List<Class<?>> repositories = new ArrayList<>();
    for (BeanDefinition candidate : candidates) {
      repositories.add(Class.forName(candidate.getBeanClassName()));
    }
    return repositories;
  }
}
