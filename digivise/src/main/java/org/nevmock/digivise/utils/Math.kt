package org.nevmock.digivise.utils

import org.nevmock.digivise.domain.model.KPI

enum class ActionType {
    DECREASE_PROPORTIONAL_CPC,    // Turunkan bid proporsional berdasarkan selisih CPC
    KEEP,                         // Biarkan tanpa perubahan
    PAUSE_KEYWORD,               // Matikan keyword / iklan ini
    DECREASE_ACOS,               // Turunkan bid berdasarkan selisih ACOS
    WAIT_FOR_MORE_CLICKS,        // Tunggu sampai klik lebih banyak
    DECREASE_ACOS_WITH_LIMIT,    // Turunkan ACOS tapi jaga di atas minimal bid
    DECREASE_SLIGHT,             // Turunkan bid sedikit
    QUEUE_EVALUATION             // Masuk antrian evaluasi
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
        "Turunkan bid proporsional dari CPC (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%)"

    ActionType.KEEP ->
        "Biarkan"

    ActionType.PAUSE_KEYWORD ->
        "Matikan keyword / iklan ini"

    ActionType.DECREASE_ACOS ->
        "Turunkan bid berdasarkan ACOS error (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%)"

    ActionType.WAIT_FOR_MORE_CLICKS ->
        "Tunggu sampai klik lebih banyak dulu (tandai evaluasi nanti)"

    ActionType.DECREASE_ACOS_WITH_LIMIT ->
        "Turunkan bid berdasarkan ACOS error (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%), tapi jangan sampai di bawah min bid"

    ActionType.DECREASE_SLIGHT ->
        "Turunkan bid sedikit (karena keduanya di atas KPI) (contoh: ${"%.1f".format(rec.adjustment!! * 100)}%)"

    ActionType.QUEUE_EVALUATION ->
        "Biarkan, tapi masuk antrian evaluasi (bisa tunggu sampai klik cukup untuk keputusan)"
}

fun formulateRecommendation(
    cpc: Double,
    acos: Double,
    klik: Double,
    kpi: KPI
): Recommendation {
    val overCpc = cpc > kpi.maxCpc
    val goodCpc = cpc <= kpi.maxCpc
    val overAcos = acos > kpi.maxAcos
    val efficientAcos = acos <= kpi.maxAcos
    val manyKlik = klik >= kpi.minKlik
    val fewKlik = klik < kpi.minKlik

    val slightAcosOver = overAcos && (acos - kpi.maxAcos) <= (kpi.maxAcos * 0.1)
    val largeAcosOver = overAcos && !slightAcosOver

    fun adjCpc() = getAdjustment(cpc - kpi.maxCpc, kpi.cpcScaleFactor)
    fun adjAcos() = getAdjustment(acos - kpi.maxAcos, kpi.acosScaleFactor)

    return when {
        // CPC tinggi & ACOS sangat tinggi & klik banyak
        overCpc && largeAcosOver && manyKlik ->
            Recommendation(ActionType.DECREASE_PROPORTIONAL_CPC, adjCpc())

        // CPC tinggi & ACOS sedikit tinggi & klik banyak
        overCpc && slightAcosOver && manyKlik ->
            Recommendation(ActionType.DECREASE_SLIGHT, adjCpc())

        // CPC tinggi & ACOS efisien & klik banyak
        overCpc && efficientAcos && manyKlik ->
            Recommendation(ActionType.KEEP)

        // CPC baik & ACOS sangat tinggi & klik banyak
        goodCpc && largeAcosOver && manyKlik ->
            Recommendation(ActionType.PAUSE_KEYWORD)

        // CPC baik & ACOS di atas & klik sedikit
        goodCpc && slightAcosOver && fewKlik ->
            Recommendation(ActionType.DECREASE_ACOS, adjAcos())

        // CPC baik & ACOS sangat tinggi & klik sedikit
        goodCpc && largeAcosOver && fewKlik ->
            Recommendation(ActionType.DECREASE_ACOS_WITH_LIMIT, adjAcos())

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
            Recommendation(ActionType.KEEP)

        else ->
            Recommendation(ActionType.KEEP)
    }
}

//fun formulateRecommendation(
//    cpc: Double,
//    acos: Double,
//    klik: Double,
//    kpi: KPI
//): Recommendation {
//    val overCpc       = cpc > kpi.maxCpc
//    val goodCpc       = cpc <= kpi.maxCpc
//    val overAcos      = acos > kpi.maxAcos
//    val efficientAcos = acos <= kpi.maxAcos
//    val manyKlik      = klik >= kpi.minKlik
//    val fewKlik       = klik <  kpi.minKlik
//
//    // definisi ACOS "sedikit di atas" = selisih ≤ 10%
//    val slightAcosOver = overAcos && (acos - kpi.maxAcos) <= (kpi.maxAcos * 0.1)
//    val largeAcosOver  = overAcos && !slightAcosOver
//
//    // helper to compute raw adjustments
//    fun adjCpc()  = getAdjustment(cpc - kpi.maxCpc, kpi.scaleFactor)
//    fun adjAcos() = getAdjustment(acos - kpi.maxAcos, kpi.scaleFactor)
//
//    // 1) initial recommendation logic
//    val initial = when {
//        overCpc && largeAcosOver && manyKlik ->
//            Recommendation(ActionType.DECREASE_PROPORTIONAL_CPC, adjCpc())
//
//        overCpc && slightAcosOver && manyKlik ->
//            Recommendation(ActionType.DECREASE_SLIGHT, adjCpc())
//
//        overCpc && efficientAcos && manyKlik ->
//            Recommendation(ActionType.KEEP)
//
//        goodCpc && largeAcosOver && manyKlik ->
//            Recommendation(ActionType.PAUSE_KEYWORD)
//
//        goodCpc && slightAcosOver && fewKlik ->
//            Recommendation(ActionType.DECREASE_ACOS, adjAcos())
//
//        goodCpc && largeAcosOver && fewKlik ->
//            Recommendation(ActionType.DECREASE_ACOS_WITH_LIMIT, adjAcos())
//
//        overCpc && overAcos && fewKlik ->
//            Recommendation(ActionType.WAIT_FOR_MORE_CLICKS)
//
//        overCpc && efficientAcos && fewKlik ->
//            Recommendation(ActionType.QUEUE_EVALUATION)
//
//        cpc == kpi.maxCpc && acos == kpi.maxAcos ->
//            Recommendation(ActionType.KEEP)
//
//        goodCpc && efficientAcos && manyKlik ->
//            Recommendation(ActionType.KEEP)
//
//        else ->
//            Recommendation(ActionType.KEEP)
//    }
//
//    // 2) clamp any decrease so resultBid >= minBidSearch
//    fun isDecreaseAction(a: ActionType) = when(a) {
//        ActionType.DECREASE_PROPORTIONAL_CPC,
//        ActionType.DECREASE_ACOS,
//        ActionType.DECREASE_ACOS_WITH_LIMIT,
//        ActionType.DECREASE_SLIGHT -> true
//        else -> false
//    }
//
//    initial.adjustmentPct?.let { pct ->
//        if (isDecreaseAction(initial.action)) {
//            val newBid = cpc * (1.0 + pct)
//            if (newBid < kpi.minBidSearch) {
//                // recalculate pct so newBid == minBidSearch
//                val clampedPct = (kpi.minBidSearch / cpc) - 1.0
//                return Recommendation(initial.action, clampedPct)
//            }
//        }
//    }
//
//    return initial
//}
