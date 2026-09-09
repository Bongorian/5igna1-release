package com.bongorian.signa1

import kotlin.math.max
import kotlin.math.min

/** Bounded preview work with fast load shedding and slow recovery. No fault state is changed. */
internal class AdaptiveLoad {
    var previewFps: Int = 24
    var thermalLevel: Int = 0
    var cooling: Boolean = false
    var expert: Boolean = false
    var renderMillis: Double = 0.0
    var relaxedSince: Long = -1
    var coolSince: Long = -1
    var nextPreviewNs: Long = 0
    var recommendedFps: Int = 24
        private set
    private var calibratedWorkBudget: Double? = null

    fun recommend(fps: Int, workBudget: Double?) {
        recommendedFps = fps.coerceIn(1,60)
        calibratedWorkBudget = workBudget?.takeIf { it.isFinite() && it > 0 }
        if (!expert) previewFps = recommendedFps
        renderMillis = 0.0
        relaxedSince = -1
        resetClock()
    }

    fun setExpert(enabled: Boolean, cameraFps: Int) {
        if (expert != enabled) {
            resetClock()
            coolSince = -1
            relaxedSince = coolSince
            previewFps = recommendedFps
        }
        expert = enabled
        if (enabled) {
            previewFps = max(1, cameraFps)
            cooling = false
        }
    }

    fun sample(
        nowMillis: Long,
        status: Int,
        batteryC: Float,
        headroom: Float,
        pixels: Long,
        passes: Int,
        constrained: Boolean,
    ) {
        var level = max(0, status)
        if (java.lang.Float.isFinite(batteryC) && batteryC > 0 && batteryC < 90)
            level =
                max(
                    level,
                    if (batteryC >= 48) 4
                    else if (batteryC >= 45) 3
                    else if (batteryC >= 42) 2 else if (batteryC >= 40) 1 else 0,
                )
        if (java.lang.Float.isFinite(headroom) && headroom >= 0)
            level =
                max(
                    level,
                    if (headroom >= 1) 3
                    else if (headroom >= .8f) 2 else if (headroom >= .6f) 1 else 0,
                )
        thermalLevel = level
        if (expert) {
            cooling = false
            relaxedSince = -1
            coolSince = relaxedSince
            return
        }
        if (level >= 4) {
            cooling = true
            coolSince = -1
        } else if (cooling) {
            if (level <= 1) {
                if (coolSince < 0) coolSince = nowMillis
                if (nowMillis - coolSince >= 30000) {
                    cooling = false
                    coolSince = -1
                }
            } else coolSince = -1
        }
        val budget = calibratedWorkBudget ?: if (constrained) 90000000.0 else 180000000.0
        var cap =
            min(
                if (calibratedWorkBudget == null && constrained) min(20,recommendedFps) else recommendedFps,
                (budget / max(1, pixels * max(1L, passes.toLong()))).toInt(),
            )
        if (renderMillis > 0) cap = min(cap, (650 / renderMillis).toInt())
        cap = min(cap, if (level >= 3) 6 else if (level == 2) 12 else if (level == 1) 20 else recommendedFps)
        var target = min(6,recommendedFps)
        for (rate in RATES) if (rate <= cap) target = rate
        if (target < previewFps) {
            previewFps = target
            relaxedSince = -1
        } else if (target > previewFps) {
            if (relaxedSince < 0) relaxedSince = nowMillis
            if (nowMillis - relaxedSince >= 15000) {
                previewFps = RATES.firstOrNull { it > previewFps && it <= target } ?: target
                relaxedSince = nowMillis
            }
        } else relaxedSince = -1
    }

    fun rendered(milliseconds: Double) {
        if (java.lang.Double.isFinite(milliseconds) && milliseconds > 0)
            renderMillis =
                if (renderMillis == 0.0) milliseconds
                else
                    renderMillis * .9 +
                        min(
                            milliseconds,
                            500.0,
                        ) * .1
    }

    fun due(cameraNs: Long): Boolean {
        return expert || nextPreviewNs == 0L || cameraNs >= nextPreviewNs
    }

    fun presented(cameraNs: Long) {
        val interval = intervalNs()
        nextPreviewNs =
            if (nextPreviewNs == 0L || cameraNs - nextPreviewNs > interval) cameraNs + interval
            else nextPreviewNs + interval
    }

    fun resetClock() {
        nextPreviewNs = 0
    }

    fun intervalNs(): Long {
        return 1000000000L / previewFps
    }

    fun cameraFps(): Int {
        return if (expert) previewFps else if (previewFps <= 15) 15 else if (previewFps <= 30) 30 else 60
    }

    companion object {
        val RATES: IntArray = intArrayOf(6, 8, 12, 15, 20, 24, 30, 60)
    }
}
