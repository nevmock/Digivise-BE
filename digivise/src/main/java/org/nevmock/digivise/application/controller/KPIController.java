package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.kpi.KPIRequestDto;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.domain.port.in.KPIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KPIController {

    private final KPIService kpiService;

    @PostMapping
    public ResponseEntity<KPIResponseDto> createKPI(KPIRequestDto kpi) {
        KPIResponseDto createdKPI = kpiService.createKPI(kpi);
        return ResponseEntity.ok().body(createdKPI);
    }

    @PostMapping("/update")
    public ResponseEntity<KPIResponseDto> updateKPI(KPIRequestDto kpi) {
        KPIResponseDto updatedKPI = kpiService.updateKPI(kpi);
        return ResponseEntity.ok().body(updatedKPI);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteKPIById(UUID kpiId) {
        kpiService.deleteKPIById(kpiId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<KPIResponseDto> getKPIById(@PathVariable UUID id) {
        KPIResponseDto kpi = kpiService.getKPIById(id);
        return ResponseEntity.ok(kpi);
    }

    @GetMapping
    public ResponseEntity<List<KPIResponseDto>> getAllKPIs() {
        List<KPIResponseDto> kpis = kpiService.getAllKPIs();
        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<KPIResponseDto> getKPIByMerchantId(@PathVariable UUID merchantId) {
        KPIResponseDto kpi = kpiService.getKPIByMerchantId(merchantId);
        return ResponseEntity.ok(kpi);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<KPIResponseDto>> getKPIsByUserId(@PathVariable UUID userId) {
        List<KPIResponseDto> kpis = kpiService.getKPIsByUserId(userId);
        return ResponseEntity.ok(kpis);
    }
}
