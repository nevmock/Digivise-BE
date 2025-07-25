package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.merchant.*;
import org.nevmock.digivise.domain.port.in.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    public ResponseEntity<MerchantResponseDto> createMerchant(@RequestBody MerchantRequestDto merchant) {
        MerchantResponseDto created = merchantService.createMerchant(merchant);

        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponseDto> getMerchantById(@PathVariable UUID id) {
        MerchantResponseDto merchant = merchantService.getMerchantById(id);
        return ResponseEntity.ok(merchant);
    }

    @GetMapping
    public ResponseEntity<List<MerchantResponseDto>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MerchantResponseDto>> getMerchantsByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(merchantService.getMerchantsByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponseDto> updateMerchant(
            @PathVariable UUID id,
            @RequestBody MerchantRequestDto updatedMerchant) {
        MerchantResponseDto merchant = merchantService.updateMerchant(id, updatedMerchant);
        return ResponseEntity.ok(merchant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable UUID id) {
        merchantService.deleteMerchant(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteMerchantByUserId(@PathVariable UUID userId) {
        merchantService.deleteMerchantByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Boolean> loginMerchant(
            @RequestBody
            MerchantLoginDto merchantLoginDto
    ) {
        Boolean merchant = merchantService.loginMerchant(merchantLoginDto.getUsername(), merchantLoginDto.getPassword(), UUID.fromString(merchantLoginDto.getMerchantId()));
        return ResponseEntity.ok(merchant);
    }

    @PostMapping("/otp-login")
    public ResponseEntity<MerchantInfoResponseDto> otpLoginMerchant(
            @RequestBody
            MerchantOtpLoginDto merchantOtpLoginDto
    ) {
        MerchantInfoResponseDto merchant = merchantService.otpLoginMerchant(merchantOtpLoginDto.getUsername(), UUID.fromString(merchantOtpLoginDto.getMerchantId()), merchantOtpLoginDto.getOtp());
        return ResponseEntity.ok(merchant);
    }

    @GetMapping("/switch")
    public ResponseEntity<MerchantResponseDto> switchMerchant(
            @RequestParam
            String merchantId
    ) {
        UUID uuid = UUID.fromString(merchantId.trim());

        MerchantResponseDto merchant = merchantService.switchMerchant(uuid);
        return ResponseEntity.ok(merchant);
    }
}
