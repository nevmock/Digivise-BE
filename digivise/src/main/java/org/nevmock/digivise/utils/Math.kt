package org.nevmock.digivise.utils

import org.nevmock.digivise.domain.model.KPI
import kotlin.math.abs

enum class ActionType {
    DECREASE_PROPORTIONAL_CPC,    // Turunkan bid proporsional berdasarkan selisih CPC
    INCREASE_BID,              // Naikkan bid
    KEEP,                         // Biarkan tanpa perubahan
    PAUSE_KEYWORD,               // Matikan keyword / iklan ini
    DECREASE_ACOS,               // Turunkan bid berdasarkan selisih ACOS
    WAIT_FOR_MORE_CLICKS,        // Tunggu sampai klik lebih banyak
    DECREASE_ACOS_WITH_LIMIT,    // Turunkan ACOS tapi jaga di atas minimal bid
    DECREASE_SLIGHT,             // Turunkan bid sedikit
    QUEUE_EVALUATION,             // Masuk antrian evaluasi
    IS_ROAS,
}

data class Recommendation(
    val action: ActionType,
    val adjustment: Double? = null
)

fun getAdjustment(diff: Double, scaleFactor: Double): Double {
    return -(diff / scaleFactor) / 100.0
}

fun renderInsight(rec: Recommendation): String = when (rec.action) {
    ActionType.DECREASE_PROPORTIONAL_CPC ->
        "Turunkan bid proporsional dari CPC (contoh: ${"%.1f".format(abs(rec.adjustment!! * 100))}%)"

    ActionType.INCREASE_BID ->
        "Naikkan bid sebanyak ${"%.1f".format(rec.adjustment!! * 100)}%"

    ActionType.KEEP ->
        "Biarkan tanpa perubahan"

    ActionType.PAUSE_KEYWORD ->
        "Matikan keyword / iklan ini"

    ActionType.DECREASE_ACOS ->
        "Turunkan bid berdasarkan ACOS error (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%)"

    ActionType.WAIT_FOR_MORE_CLICKS ->
        "Tunggu sampai klik lebih banyak dulu (tandai evaluasi nanti)"

    ActionType.DECREASE_ACOS_WITH_LIMIT ->
        "Turunkan bid berdasarkan ACOS error (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%), tapi jangan sampai di bawah min bid"

    ActionType.DECREASE_SLIGHT ->
        "Turunkan bid sedikit (contoh: ${"%.1f".format(abs(rec.adjustment!! * 100))}%)"

    ActionType.QUEUE_EVALUATION ->
        "Biarkan, tapi masuk antrian evaluasi (bisa tunggu sampai klik cukup untuk keputusan)"

    ActionType.IS_ROAS ->
        // Roas bentuknya adalah rupiah, jadi persen dari current budget dan rupiahnya
        "Targetkan budget menjadi ${"%.1f".format(rec.adjustment!! * 100)}% dari total revenue yang dihasilkan dari ROAS KPI (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%)"
}

fun formulateRecommendation(
    cpc: Double,
    acos: Double,
    klik: Double,
    kpi: KPI,
    roas: Double? = null,
    budget: Double? = null,
): Recommendation {
    val overCpc = cpc > kpi.maxCpc
    val goodCpc = cpc <= kpi.maxCpc
    val overAcos = acos > kpi.maxAcos
    val efficientAcos = acos <= kpi.maxAcos
    val manyKlik = klik >= kpi.minKlik
    val fewKlik = klik < kpi.minKlik

    val slightAcosOver = overAcos && (acos - kpi.maxAcos) <= (kpi.maxAcos * 0.1)
    val largeAcosOver = overAcos && !slightAcosOver

    if (budget != null && roas != null && kpi.roasKpi != null) {
        return Recommendation(
            ActionType.IS_ROAS,
            calculateRoas(kpi.roasKpi, roas, budget)
        )
    }

    return when {
        // CPC tinggi & ACOS sangat tinggi & klik banyak
        overCpc && largeAcosOver && manyKlik ->
            Recommendation(ActionType.DECREASE_PROPORTIONAL_CPC, getAdjustment(
                acos - kpi.maxAcos,
                kpi.acosScaleFactor
            ))

        // CPC tinggi & ACOS sedikit tinggi & klik banyak
        overCpc && slightAcosOver && manyKlik ->
            Recommendation(ActionType.DECREASE_SLIGHT, getAdjustment(
                acos - kpi.maxAcos,
                kpi.acosScaleFactor
            ))

        // CPC tinggi & ACOS efisien & klik banyak
        overCpc && efficientAcos && manyKlik ->
            Recommendation(ActionType.KEEP)

        // CPC baik & ACOS sangat tinggi & klik banyak
        goodCpc && largeAcosOver && manyKlik ->
            Recommendation(ActionType.PAUSE_KEYWORD)

        // CPC baik & ACOS di atas & klik sedikit
        goodCpc && slightAcosOver && fewKlik ->
            Recommendation(ActionType.DECREASE_ACOS, getAdjustment(
                acos - kpi.maxAcos,
                kpi.acosScaleFactor
            ))

        // CPC baik & ACOS sangat tinggi & klik sedikit
        goodCpc && largeAcosOver && fewKlik ->
            Recommendation(ActionType.DECREASE_ACOS_WITH_LIMIT, getAdjustment(
                acos - kpi.maxAcos,
                kpi.acosScaleFactor
            ))

        // CPC tinggi & ACOS tinggi & klik sedikit
        overCpc && overAcos && fewKlik ->
            Recommendation(ActionType.WAIT_FOR_MORE_CLICKS)

        // CPC tinggi & ACOS efisien & klik sedikit
        overCpc && efficientAcos && fewKlik ->
            Recommendation(ActionType.QUEUE_EVALUATION)

        // CPC == KPI & ACOS == KPI
        cpc == kpi.maxCpc && acos == kpi.maxAcos ->
            Recommendation(ActionType.KEEP)

        // CPC baik & ACOS efisien & klik banyak
        goodCpc && efficientAcos && manyKlik ->
            Recommendation(ActionType.INCREASE_BID, getAdjustment(acos - kpi.maxAcos, kpi.acosScaleFactor))

        else ->
            Recommendation(ActionType.KEEP)
    }
}

fun calculateRoas(
    roasKpi : Double,
    roasReal : Double,
    currentBudget : Double,
): Double {
    val revenue = roasReal * currentBudget
    val targetBudget = revenue / roasKpi

    return targetBudget
}