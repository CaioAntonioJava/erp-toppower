package br.com.toppower.erp_toppower.user.controller;

import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.user.dto.ChangePasswordRequest;
import br.com.toppower.erp_toppower.user.dto.ResetPasswordRequest;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetailsImpl principal,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.uuid(), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
