package org.nevmock.digivise;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.utils.Recommendation;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.nevmock.digivise.utils.MathKt.formulateRecommendation;
import static org.nevmock.digivise.utils.MathKt.renderInsight;

class KPIProcessingUnitTest {

    private static KPI
        kpi = KPI.builder()
                .id(UUID.randomUUID())
                .merchant(null)
                .user(null)
                .maxCpc(400.0)
                .maxAcos(15.0)
                .scaleFactor(10.0)
                .maxAdjustment(0.0)
                .minAdjustment(0.0)
                .maxKlik(150.0)
                .minKlik(30.0)
                .minBidSearch(200.0)
                .minBidReco(150.0)
                .multiplier(1.0)
                .build();

    public static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(500.0, 25.0, 150.0, "Turunkan bid proporsional dari CPC (contoh: -10.0%)"),
                Arguments.of(450.0, 12.0, 100.0, "Biarkan"),
                Arguments.of(350.0, 28.0, 170.0, "Matikan keyword / iklan ini"),
                Arguments.of(370.0, 20.0, 20.0,  "Turunkan bid berdasarkan ACOS error (contoh: -2.5%), tapi jangan sampai di bawah min bid"),
                Arguments.of(400.0, 15.0, 80.0,  "Biarkan"),
                Arguments.of(410.0, 18.0, 20.0,  "Tunggu sampai klik lebih banyak dulu (tandai evaluasi nanti)"),
                Arguments.of(390.0, 40.0, 10.0,  "Turunkan bid berdasarkan ACOS error (contoh: -12.5%), tapi jangan sampai di bawah min bid"),
                Arguments.of(300.0, 10.0, 300.0, "Biarkan"),
                Arguments.of(420.0, 16.0, 160.0, "Turunkan bid sedikit (karena keduanya di atas KPI) (contoh: -2.0%)"),
                Arguments.of(410.0, 14.0, 25.0,  "Biarkan, tapi masuk antrian evaluasi (bisa tunggu sampai klik cukup untuk keputusan)")
        );
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void testAllTenScenarios(double cpc, double acos, double klik, String expectedInsight) {
        Recommendation rec = formulateRecommendation(cpc, acos, klik, kpi);
        String insight = renderInsight(rec);

        assertEquals(expectedInsight, insight);
    }
}
