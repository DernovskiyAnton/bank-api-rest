package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthResponseDto;
import com.example.bankcards.dto.CreateUserRequestDto;
import com.example.bankcards.dto.LoginRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ErrorResponse;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Operation(
            summary = "User login",
            description = "Authenticate user with username and password. Returns JWT token and user information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),

            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtTokenProvider.generateToken(user);

        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        return ResponseEntity.ok(AuthResponseDto.of(token, userDto));
    }

    @Operation(
            summary = "Register new user",
            description = "Create a new user account with USER role. Returns JWT token and user information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Username or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody CreateUserRequestDto request) {

        UserDto userDto = userService.createUser(request);

        User user = userRepository.findById(userDto.id())
                .orElseThrow(() -> new IllegalStateException("User not found after creation"));

        String token = jwtTokenProvider.generateToken(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AuthResponseDto.of(token, userDto));
    }
}