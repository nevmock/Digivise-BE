package org.nevmock.digivise.application.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.application.dto.merchant.MerchantInfoResponseDto;
import org.nevmock.digivise.application.dto.merchant.MerchantRequestDto;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.MerchantService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.nevmock.digivise.infrastructure.adapter.security.PasswordEncryptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.nevmock.digivise.utils.UtilsKt.getCurrentUser;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final KPIRepository kpiRepository;

    private String password;

    @Override
    public MerchantResponseDto createMerchant(MerchantRequestDto merchant) {
        User user = Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new RuntimeException("User not found!"));


        Merchant newMerchant = new Merchant();

        newMerchant.setId(UUID.randomUUID());
        newMerchant.setName(merchant.getName());
        newMerchant.setUser(user);
        newMerchant.setMerchantName(null);
        newMerchant.setMerchantShopeeId(null);
        newMerchant.setFactoryAddress(merchant.getFactoryAddress());
        newMerchant.setOfficeAddress(merchant.getOfficeAddress());
        newMerchant.setSectorIndustry(merchant.getSectorIndustry());
        newMerchant.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        newMerchant.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        KPI kpi = KPI
                .builder()
                .user(user)
                .merchant(newMerchant)
                .maxAcos(0.0)
                .maxCpc(0.0)
                .maxAdjustment(0.0)
                .multiplier(0.0)
                .minBidSearch(0.0)
                .maxKlik(0.0)
                .minKlik(0.0)
                .cpcScaleFactor(0.0)
                .acosScaleFactor(0.0)
                .id(UUID.randomUUID())
                .minAdjustment(0.0)
                .minBidReco(0.0)
                .build();

        merchantRepository.save(newMerchant);
        kpiRepository.save(kpi);

        return toDto(newMerchant, kpi);
    }

    @Override
    public MerchantResponseDto getMerchantById(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + merchantId));

        KPI kpi = kpiRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("KPI not found with Merchant ID: " + merchantId));

        return toDto(merchant, kpi);
    }

    @Override
    public List<MerchantResponseDto> getAllMerchants() {
        List<Merchant> merchants = merchantRepository.findAll();
        List<KPI> kpiList = kpiRepository.findAll();

        Map<UUID, KPI> merchantIdToKpi = kpiList.stream()
                .collect(Collectors.toMap(k -> k.getMerchant().getId(), k -> k));

        return merchants.stream()
                .map(m -> toDto(m, merchantIdToKpi.get(m.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchantResponseDto> getMerchantsByUserId(UUID userId) {
        List<Merchant> merchants = merchantRepository.findByUserId(userId);
        List<KPI> kpiList = kpiRepository.findByUserId(userId);

        Map<UUID, KPI> merchantIdToKpi = kpiList.stream()
                .collect(Collectors.toMap(k -> k.getMerchant().getId(), k -> k));

        return merchants.stream()
                .map(m -> toDto(m, merchantIdToKpi.get(m.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public MerchantResponseDto updateMerchant(UUID merchantId, MerchantRequestDto updatedMerchant) {
        Optional<Merchant> existingMerchant = merchantRepository.findById(merchantId);

        KPI existingKPI = kpiRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("KPI not found with Merchant ID: " + merchantId));

        if (existingMerchant.isEmpty()) {
            throw new RuntimeException("Merchant not found with ID: " + merchantId);
        }

        Merchant merchantToUpdate = existingMerchant.get();

        merchantToUpdate.setName(updatedMerchant.getName());
        merchantToUpdate.setOfficeAddress(updatedMerchant.getOfficeAddress());
        merchantToUpdate.setFactoryAddress(updatedMerchant.getFactoryAddress());
        merchantToUpdate.setSectorIndustry(updatedMerchant.getSectorIndustry());
        merchantToUpdate.setUser(Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new RuntimeException("User not found!")));
        merchantToUpdate.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        merchantRepository.save(merchantToUpdate);



        return toDto(merchantToUpdate, existingKPI);
    }

    @Override
    public void deleteMerchant(UUID merchantId) {
        merchantRepository.deleteById(merchantId);
    }

    @Override
    public void deleteMerchantByUserId(UUID userId) {
        List<Merchant> merchants = merchantRepository.findByUserId(userId);
        for (Merchant merchant : merchants) {
            merchantRepository.deleteById(merchant.getId());
        }
    }

    private MerchantResponseDto toDto(Merchant merchant, KPI kpi) {
        KPIResponseDto kpiResp = KPIResponseDto
                .builder()
                .id(kpi.getId())
                .merchantId(kpi.getMerchant().getId())
                .userId(kpi.getUser().getId())
                .maxCpc(kpi.getMaxCpc())
                .maxAcos(kpi.getMaxAcos())
                .cpcScaleFactor(kpi.getCpcScaleFactor())
                .acosScaleFactor(kpi.getAcosScaleFactor())
                .maxAdjustment(kpi.getMaxAdjustment())
                .minAdjustment(kpi.getMinAdjustment())
                .maxKlik(kpi.getMaxKlik())
                .minKlik(kpi.getMinKlik())
                .minBidSearch(kpi.getMinBidSearch())
                .minBidReco(kpi.getMinBidReco())
                .multiplier(kpi.getMultiplier())
                .build();

        return MerchantResponseDto.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .merchantName(merchant.getMerchantName())
                .merchantShopeeId(merchant.getMerchantShopeeId())
                .createdAt(merchant.getCreatedAt())
                .userId(merchant.getUser().getId())
                .kpi(
                    kpiResp
                )
                .lastLogin(merchant.getLastLogin())
                .officeAddress(merchant.getOfficeAddress())
                .factoryAddress(merchant.getFactoryAddress())
                .lastLogin(merchant.getLastLogin())
                .sectorIndustry(merchant.getSectorIndustry()).build();

    }

    @Override
    public Boolean loginMerchant(String email, String password) {
        HttpClient httpClient = HttpClient.newHttpClient();

        String jsonBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email,
                password
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://103.150.116.30:1337/api/v1/shopee-seller/login"))
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to login merchant: " + response.body());
            }

            this.password = PasswordEncryptor.INSTANCE.encrypt(
                    password
            );

            return response.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to login merchant", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MerchantInfoResponseDto otpLoginMerchant(String username, UUID merchantId, String otpCode) {

        User user = Objects.requireNonNull(getCurrentUser(userRepository)).orElse(null);
        if (user == null) return null;

        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        String jsonBody = String.format("{\"username\":\"%s\",\"otp\":\"%s\"}", username, otpCode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://103.150.116.30:1337/api/v1/shopee-seller/otp-email"))
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null; // gagal login
            }

            MerchantInfoResponseDto result = objectMapper.readValue(response.body(), MerchantInfoResponseDto.class);
            String newShopeeId = String.valueOf(result.getData().getData().getShopId());

            Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
            if (merchantOpt.isEmpty()) return null;

            Merchant merchantToUpdate = merchantOpt.get();

            // Jika merchant sudah punya Shopee ID yang sama, langsung return
            if (merchantToUpdate.getMerchantShopeeId() != null &&
                    merchantToUpdate.getMerchantShopeeId().equals(newShopeeId)) {

                user.setActiveMerchant(merchantToUpdate);
                userRepository.save(user);
                    return result;
            }

            // Cek kalau Shopee ID sudah dipakai merchant lain
            Optional<Merchant> existingShopee = merchantRepository.findByShopeeMerchantId(newShopeeId);

            if (existingShopee.isPresent() && !existingShopee.get().getId().equals(merchantId)) {
                // Sudah dipakai merchant lain, skip update Shopee ID
                // Bisa log info di sini kalau perlu
                merchantToUpdate.setMerchantShopeeId(newShopeeId);
            } else {
                // Aman untuk set Shopee ID baru
                merchantToUpdate.setMerchantShopeeId(newShopeeId);
            }

            merchantToUpdate.setMerchantName(result.getData().getData().getName());
            merchantToUpdate.setPassword(password); // Pastikan variable `password` terisi valid
            merchantToUpdate.setLastLogin(Timestamp.from(Instant.now()));
            merchantRepository.save(merchantToUpdate);

            user.setActiveMerchant(merchantToUpdate);
            userRepository.save(user);

            return result;

        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    @Override
    public MerchantResponseDto switchMerchant(UUID merchantId) {
        User user = Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (merchantOpt.isEmpty()) {
            throw new RuntimeException("Merchant not found with ID: " + merchantId);
        }

        Merchant merchant = merchantOpt.get();

        if (merchant.getMerchantShopeeId() == null || merchant.getMerchantShopeeId().isEmpty()) {
            throw new RuntimeException("Merchant Shopee ID is not set for Merchant ID: " + merchantId);
        }

        user.setActiveMerchant(merchant);
        merchant.setLastLogin(Timestamp.from(Instant.now()));
        userRepository.save(user);

        return toDto(merchant,
                kpiRepository.findByMerchantId(merchantId)
                        .orElseThrow(() -> new RuntimeException("KPI not found with Merchant ID: " + merchantId)));
    }
}
