package com.school_guardian.ms_iam.application.mapper;

import com.school_guardian.ms_iam.application.dto.AcceptTermsRequest;
import com.school_guardian.ms_iam.domain.model.TermsChannel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TermsAcceptanceMapperTest {

    @Test
    void mapsTheRequestChannelAndTheClientIpIntoTheCommand() {
        var student = UUID.randomUUID();
        var request = new AcceptTermsRequest("2026.08", List.of(student), "WEB");

        var command = TermsAcceptanceMapper.toCommand(request, "token", "190.24.8.15");

        assertEquals("token", command.accessToken());
        assertEquals("2026.08", command.termsVersion());
        assertEquals(List.of(student), command.studentProfileIds());
        assertEquals("190.24.8.15", command.ipAddress());
        assertEquals(TermsChannel.WEB, command.channel());
    }

    @Test
    void aRequestWithoutChannelIsRecordedAsUnspecifiedNotAsMobile() {
        var request = new AcceptTermsRequest("2026.08", null, null);

        var command = TermsAcceptanceMapper.toCommand(request, "token", "190.24.8.15");

        assertEquals(TermsChannel.UNSPECIFIED, command.channel());
    }
}
