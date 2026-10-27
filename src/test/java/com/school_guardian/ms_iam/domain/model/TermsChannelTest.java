package com.school_guardian.ms_iam.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TermsChannelTest {

    @Test
    void parsesKnownChannelsIgnoringCase() {
        assertEquals(TermsChannel.WEB, TermsChannel.from("web"));
        assertEquals(TermsChannel.WEB, TermsChannel.from("WEB"));
        assertEquals(TermsChannel.MOBILE, TermsChannel.from(" Mobile "));
    }

    @Test
    void missingChannelIsUnspecified() {
        assertEquals(TermsChannel.UNSPECIFIED, TermsChannel.from(null));
        assertEquals(TermsChannel.UNSPECIFIED, TermsChannel.from("  "));
    }

    @Test
    void unknownChannelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TermsChannel.from("tablet"));
    }

    @Test
    void storedValuesKeepTheHistoricalLowerCaseMobile() {
        assertEquals("mobile", TermsChannel.MOBILE.value());
        assertEquals("web", TermsChannel.WEB.value());
        assertEquals("unspecified", TermsChannel.UNSPECIFIED.value());
    }
}
