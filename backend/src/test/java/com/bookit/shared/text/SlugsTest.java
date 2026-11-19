package com.bookit.shared.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugsTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            Barber Shop                  | barber-shop
            Zażółć gęślą jaźń            | zazolc-gesla-jazn
            ŁÓDŹ                         | lodz
            Fryzjer Łódź & Spa!          | fryzjer-lodz-spa
            '  --Studio   Urody--  '     | studio-urody
            Salon No.1                   | salon-no-1
            Café Élégance                | cafe-elegance
            Straße                       | stra-e
            '!!!'                        | ''
            -                            | ''
            '- a -'                      | a
            """)
    void convertsTextToSlug(String text, String expected) {
        assertThat(Slugs.toSlug(text)).isEqualTo(expected);
    }

    @Test
    void handlesLongRunsOfSeparatorsQuickly() {
        String text = "-".repeat(100_000) + "a" + "-".repeat(100_000);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertThat(Slugs.toSlug(text)).isEqualTo("a"));
    }
}
