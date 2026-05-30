package com.capstone.data.read;

/**
 * Column labels expected by Spring JDBC when mapping rows into Java records via {@code
 * JdbcClient#query(Class)}.
 *
 * <p>Each record component {@code fooBar} must appear in the result set as {@code foo_bar} (use
 * {@code SELECT source_col AS foo_bar} when the source column name differs).
 */
final class JdbcRecordColumns {

  private JdbcRecordColumns() {}

  static String toSnakeCase(String javaName) {
    StringBuilder sb = new StringBuilder(javaName.length() + 4);
    for (int i = 0; i < javaName.length(); i++) {
      char c = javaName.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          sb.append('_');
        }
        sb.append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
