package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.AuthDtos.Profile;
import com.web.motoridelab3api.dto.AuthDtos.ProfileUpdate;
import com.web.motoridelab3api.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {
    private final AuthService auth;

    @GetMapping
    public ResponseEntity<Profile> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.me());
    }

    @PutMapping
    public ResponseEntity<Profile> update(@Valid @RequestBody ProfileUpdate r) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.update(r));
    }
}
