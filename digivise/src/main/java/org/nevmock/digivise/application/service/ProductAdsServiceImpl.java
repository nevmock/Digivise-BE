package org.nevmock.digivise.application.service;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.mongo.ads.ProductAds;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.ProductAdsRepository;
import org.nevmock.digivise.utils.Recommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nevmock.digivise.utils.MathKt;

@Service
public class ProductAdsServiceImpl implements ProductAdsService {

    @Autowired
    ProductAdsRepository productAdsRepository;

    @Autowired
    KPIRepository kpiRepository;

    @Autowired
    MerchantRepository merchantRepository;

    public List<ProductAdsResponseDto> findAll() {
        return productAdsRepository.findAll().stream()
                .flatMap(ad -> {
                    String id = ad.getId();
                    String shopeeMerchantId = ad.getShopId();
                    String toLoc = ad.getTo();
                    String fromLoc = ad.getFrom();
                    LocalDateTime createdAt = ad.getCreatedAt();
                    if (ad.getData() != null && ad.getData().getData() != null) {
                        return ad.getData().getData().getEntry_list().stream()
                                .map(e -> {
                                    ProductAdsResponseDto dto = new ProductAdsResponseDto();
                                    dto.setId(id);
                                    dto.setShopeeMerchantId(shopeeMerchantId);
                                    dto.setFrom(fromLoc);
                                    dto.setTo(toLoc);
                                    dto.setCreatedAt(createdAt);
                                    dto.setCampaignId(e.getCampaign().getCampaignId());
                                    dto.setAcos(e.getReport().getBroad_gmv());
                                    dto.setCpc(e.getReport().getCpc());
                                    return dto;
                                });
                    }
                    ProductAdsResponseDto baseDto = new ProductAdsResponseDto();
                    baseDto.setId(id);
                    baseDto.setShopeeMerchantId(shopeeMerchantId);
                    baseDto.setFrom(fromLoc);
                    baseDto.setTo(toLoc);
                    baseDto.setCreatedAt(createdAt);
                    return List.of(baseDto).stream();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductAdsResponseDto> findById(String id) {
        return Optional.empty();
    }

    @Override
    public List<ProductAdsResponseDto> findByShopId(String shopId) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtBetween(String from, String to) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtGreaterThanEqual(String from) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtLessThanEqual(String to) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByFromAndTo(String from, String to) {
        return List.of();
    }

    @Override
    public Page<ProductAdsResponseDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        List<ProductAds> allAds = productAdsRepository
                .findByShopIdAndCreatedAtBetween(shopId, from, to, Pageable.unpaged())
                .getContent();

        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + shopId));

        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for Merchant ID: " + merchant.getId()));

        List<ProductAdsResponseDto> allDtos = allAds.stream()
                .flatMap(ad -> {
                    String id = ad.getId();
                    String shopeeMerchantId = ad.getShopId();
                    String toLoc = ad.getTo();
                    String fromLoc = ad.getFrom();
                    LocalDateTime createdAt = ad.getCreatedAt();

                    if (ad.getData() != null && ad.getData().getData() != null) {
                        return ad.getData().getData().getEntry_list().stream()
                                .map(e -> {
                                    ProductAdsResponseDto dto = new ProductAdsResponseDto();
                                    dto.setId(id);
                                    dto.setShopeeMerchantId(shopeeMerchantId);
                                    dto.setFrom(fromLoc);
                                    dto.setTo(toLoc);
                                    dto.setCreatedAt(createdAt);
                                    dto.setCampaignId(e.getCampaign().getCampaignId());
                                    dto.setAcos(e.getReport().getBroad_gmv());
                                    dto.setCpc(e.getReport().getCpc());
                                    dto.setDailyBudget(e.getCampaign().getDailyBudget());
                                    dto.setImage(e.getImage());
                                    dto.setTitle(e.getTitle());
                                    dto.setClick(e.getReport().getClick());
                                    dto.setCtr(e.getReport().getCtr());
                                    return dto;
                                });
                    }

                    ProductAdsResponseDto baseDto = new ProductAdsResponseDto();
                    baseDto.setId(id);
                    baseDto.setShopeeMerchantId(shopeeMerchantId);
                    baseDto.setFrom(fromLoc);
                    baseDto.setTo(toLoc);
                    baseDto.setCreatedAt(createdAt);
                    return Stream.of(baseDto);
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allDtos.size());

        double averageCpc = allDtos.stream()
                .mapToDouble(ProductAdsResponseDto::getCpc)
                .average()
                .orElse(0.0);

        if (averageCpc > kpi.getMaxCpc()) {
            allDtos.forEach(dto -> {
                Recommendation rec = MathKt.formulateRecommendation(
                        dto.getCpc(),
                        dto.getAcos(),
                        dto.getClick(),
                        kpi
                );

                String insight = MathKt.renderInsight(rec);

                dto.setInsight(insight);
            });
        }

        List<ProductAdsResponseDto> pageContent = allDtos.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allDtos.size());
    }
}
