package com.example.bankcards.controller;

import com.example.bankcards.dto.BlockCardRequestDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequestDto;
import com.example.bankcards.exception.ErrorResponse;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Bank card management endpoints")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Get user cards", description = "Retrieve paginated list of current user's cards")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<CardDto>> getMyCards(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<CardDto> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get card by ID", description = "Retrieve specific card by ID (user can only access own cards)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found"),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardDto> getCardById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        CardDto card = cardService.getCardById(id, userId);
        return ResponseEntity.ok(card);
    }

    @Operation(summary = "Create new card", description = "Create a new bank card for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequestDto request) {
        Long userId = getCurrentUserId();
        CardDto card = cardService.createCard(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @Operation(summary = "Block card", description = "Block user's own card with specified reason")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card blocked successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockCard(@PathVariable Long id, @Valid @RequestBody BlockCardRequestDto request) {
        Long userId = getCurrentUserId();
        cardService.blockCard(id, request.reason(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all cards (Admin)", description = "Retrieve all cards in the system. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDto>> getAllCards(Pageable pageable) {
        Page<CardDto> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Activate card (Admin)", description = "Activate blocked card. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card activated successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete card (Admin)", description = "Permanently delete card from system. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }


}
