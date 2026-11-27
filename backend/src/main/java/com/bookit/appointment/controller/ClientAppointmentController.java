package com.bookit.appointment.controller;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.dto.CreateAppointmentRequest;
import com.bookit.appointment.service.BookingService;
import com.bookit.appointment.service.BookingService.BookingResult;
import com.bookit.auth.security.CurrentUserId;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointments")
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class ClientAppointmentController {

    private final BookingService bookingService;

    @ApiResponse(responseCode = "201", description = "Appointment booked")
    @ApiResponse(responseCode = "200", description = "Same request repeated with the same Idempotency-Key")
    @ApiResponse(
            responseCode = "409",
            description = "code SLOT_TAKEN: the time is no longer free for this employee; "
                    + "code CLIENT_OVERLAP: the client already has another appointment at this time",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = "code IDEMPOTENCY_KEY_REUSED: this Idempotency-Key was already used for a different booking",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(version = "1")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @CurrentUserId UUID clientId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateAppointmentRequest request) {
        BookingResult result = bookingService.book(clientId, idempotencyKey, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.appointment());
    }
}
