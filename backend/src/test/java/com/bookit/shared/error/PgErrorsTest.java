package com.bookit.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.shared.error.PgErrors.Violation;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class PgErrorsTest {

    @Test
    void readsConstraintNameFromHibernateException() {
        SQLException sql = new SQLException("duplicate key", PgErrors.UNIQUE_VIOLATION);
        ConstraintViolationException hibernate = new ConstraintViolationException("insert", sql, "uk_example");

        assertThat(PgErrors.classify(new DataIntegrityViolationException("wrapped", hibernate)))
                .contains(new Violation(PgErrors.UNIQUE_VIOLATION, "uk_example"));
    }

    @Test
    void fallsBackToConstraintNameInSqlMessage() {
        SQLException sql = new SQLException(
                "ERROR: conflicting key value violates exclusion constraint \"ex_example\"\n  Detail: ...",
                PgErrors.EXCLUSION_VIOLATION);
        ConstraintViolationException hibernate = new ConstraintViolationException("insert", sql, null);

        assertThat(PgErrors.classify(new DataIntegrityViolationException("wrapped", hibernate)))
                .contains(new Violation(PgErrors.EXCLUSION_VIOLATION, "ex_example"));
    }

    @Test
    void emptyWithoutSqlExceptionInCauseChain() {
        assertThat(PgErrors.classify(new DataIntegrityViolationException("no cause"))).isEmpty();
    }

    @Test
    void emptyWhenConstraintNameIsUnknown() {
        SQLException sql = new SQLException("ERROR: value too long", "22001");

        assertThat(PgErrors.classify(new DataIntegrityViolationException("wrapped", sql))).isEmpty();
    }

    @Test
    void violationMatchesOnlyBothStateAndName() {
        Violation violation = new Violation(PgErrors.EXCLUSION_VIOLATION, "ex_example");

        assertThat(violation.is(PgErrors.EXCLUSION_VIOLATION, "ex_example")).isTrue();
        assertThat(violation.is(PgErrors.UNIQUE_VIOLATION, "ex_example")).isFalse();
        assertThat(violation.is(PgErrors.EXCLUSION_VIOLATION, "ex_other")).isFalse();
    }
}
