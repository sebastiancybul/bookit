package com.bookit.business.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BusinessTest {

    private static final String PUBLIC_ID = "k3f9x2ab";

    @Test
    void slugJoinsPublicIdNameAndCity() {
        Business business = business("Fryzjer Łódź & Spa", "Kraków");

        assertThat(business.getSlug()).isEqualTo("k3f9x2ab-fryzjer-lodz-spa-krakow");
    }

    @Test
    void slugSkipsPartsThatAreEmptyAfterConversion() {
        Business business = business("!!!", "Kraków");

        assertThat(business.getSlug()).isEqualTo("k3f9x2ab-krakow");
    }

    @Test
    void slugOfExactlyMaxLengthIsKept() {
        String name = "a".repeat(50) + " " + "b".repeat(49);
        Business business = business(name, "c".repeat(10));

        assertThat(business.getSlug())
                .hasSize(120)
                .isEqualTo(PUBLIC_ID + "-" + "a".repeat(50) + "-" + "b".repeat(49) + "-" + "c".repeat(10));
    }

    @Test
    void slugCutInsideWordDropsThatWord() {
        String name = "a".repeat(50) + " " + "b".repeat(49);
        Business business = business(name, "c".repeat(11));

        assertThat(business.getSlug()).isEqualTo(PUBLIC_ID + "-" + "a".repeat(50) + "-" + "b".repeat(49));
    }

    @Test
    void slugCutRightBeforeDashKeepsFullMaxLength() {
        String name = "a".repeat(50) + " " + "b".repeat(49);
        Business business = business(name, "c".repeat(10) + " ddd");

        assertThat(business.getSlug())
                .hasSize(120)
                .isEqualTo(PUBLIC_ID + "-" + "a".repeat(50) + "-" + "b".repeat(49) + "-" + "c".repeat(10));
    }

    @Test
    void emailIsStrippedAndLowercased() {
        Business business = business("Barber", "Kraków", "  Kontakt@Barber.PL ");

        assertThat(business.getEmail()).isEqualTo("kontakt@barber.pl");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void blankEmailBecomesNull(String email) {
        Business business = business("Barber", "Kraków", email);

        assertThat(business.getEmail()).isNull();
    }

    @Test
    void updateRecomputesSlugAndKeepsPublicId() {
        Business business = business("Barber Shop", "Kraków", "kontakt@barber.pl");

        business.update("Studio Urody Żółta", null, null, "123456789", "", "Piotrkowska 10", "Łódź", "90-001");

        assertThat(business.getPublicId()).isEqualTo(PUBLIC_ID);
        assertThat(business.getSlug()).isEqualTo("k3f9x2ab-studio-urody-zolta-lodz");
        assertThat(business.getCityKey()).isEqualTo("lodz");
        assertThat(business.getEmail()).isNull();
    }

    @Test
    void textFieldsAreStrippedAndBlankDescriptionBecomesNull() {
        Business business = new Business(
                UUID.randomUUID(), PUBLIC_ID, "  Barber Shop ", "   ", null, " 123456789 ", null,
                " Floriańska 1 ", " Kraków ", " 31-019 ");

        assertThat(business.getName()).isEqualTo("Barber Shop");
        assertThat(business.getDescription()).isNull();
        assertThat(business.getPhone()).isEqualTo("123456789");
        assertThat(business.getStreet()).isEqualTo("Floriańska 1");
        assertThat(business.getCity()).isEqualTo("Kraków");
        assertThat(business.getCityKey()).isEqualTo("krakow");
        assertThat(business.getPostalCode()).isEqualTo("31-019");
    }

    private static Business business(String name, String city) {
        return business(name, city, null);
    }

    private static Business business(String name, String city, String email) {
        return new Business(
                UUID.randomUUID(), PUBLIC_ID, name, null, null, "123456789", email, "Floriańska 1", city, "31-019");
    }
}
