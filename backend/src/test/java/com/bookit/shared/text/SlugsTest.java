package com.bookit.shared.text;

import static org.assertj.core.api.Assertions.assertThat;

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
            """)
    void convertsTextToSlug(String text, String expected) {
        assertThat(Slugs.toSlug(text)).isEqualTo(expected);
    }
}
