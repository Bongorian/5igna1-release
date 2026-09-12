package com.bongorian.signa1

/** Keep audit observations reproducible while the next contract is being considered. */
class TimeModelAuditTest {
    @org.junit.Test fun characterizeCurrentTimeModel() {
        TimeModelAuditCheck.main(arrayOf("build/reports/time-model-audit.md"))
    }
}
