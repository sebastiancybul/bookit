package com.bookit.shared.error;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;

public final class PgErrors {

    public static final String UNIQUE_VIOLATION = "23505";
    public static final String EXCLUSION_VIOLATION = "23P01";

    private static final Pattern CONSTRAINT_IN_MESSAGE = Pattern.compile("constraint \"([^\"]+)\"");

    private PgErrors() {
    }

    public record Violation(String sqlState, String constraintName) {

        public boolean is(String expectedSqlState, String expectedConstraintName) {
            return expectedSqlState.equals(sqlState) && expectedConstraintName.equals(constraintName);
        }
    }

    public static Optional<Violation> classify(Throwable ex) {
        String constraintName = null;
        SQLException sqlException = null;
        for (Throwable current = ex; current != null; current = nextCause(current)) {
            if (constraintName == null && current instanceof ConstraintViolationException violation) {
                constraintName = violation.getConstraintName();
            }
            if (sqlException == null && current instanceof SQLException sql) {
                sqlException = sql;
            }
        }
        if (sqlException == null || sqlException.getSQLState() == null) {
            return Optional.empty();
        }
        if (constraintName == null) {
            constraintName = constraintFromMessage(sqlException.getMessage());
        }
        if (constraintName == null) {
            return Optional.empty();
        }
        return Optional.of(new Violation(sqlException.getSQLState(), constraintName));
    }

    private static String constraintFromMessage(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = CONSTRAINT_IN_MESSAGE.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Throwable nextCause(Throwable current) {
        Throwable cause = current.getCause();
        return cause == current ? null : cause;
    }
}
