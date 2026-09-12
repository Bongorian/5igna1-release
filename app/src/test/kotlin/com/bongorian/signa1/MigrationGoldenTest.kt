package com.bongorian.signa1

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

/* Fixed outputs recorded from the Java implementation before the Kotlin migration. */
class MigrationGoldenTest {
    @org.junit.Test
    @Throws(Exception::class)
    fun preservesLegacyFaultFramesAndRawBytes() {
        val raw = MessageDigest.getInstance("SHA-256")
        val frames = MessageDigest.getInstance("SHA-256")
        // Frozen Java evidence uses the legacy model. TimeContractTest separately checks current semantics
        // and unchanged static mechanisms. Expected Java hashes must remain unchanged.
        for (id in 0..Effects.CRT) for (level in floatArrayOf(0f, .25f, .8f, 1f)) {
            val state = EffectState.defaults().single(id).amount(level)
            val model = LegacyFaultModelReference(123456)
            val input = LegacyFaultModelReference.Inputs()
            val config = FaultConfig.defaults()
            for (n in 0..119) {
                input.sensorNs = 1 + n * 16666667L
                model.advance(n / 60.0, input, config)
            }
            val frame = model.apply(state.snapshot(true, 0), config)
            frames.update(frame.describe().toByteArray(StandardCharsets.UTF_8))
            for (size in
                arrayOf<IntArray>(intArrayOf(1, 1), intArrayOf(3, 5), intArrayOf(32, 24))) {
                val bytes = ByteArray(size[0] * size[1] * 2)
                for (n in 0 until bytes.size / 2) RawGlitch.write(bytes, n, 256 + (n * 71) % 3500)
                raw.update(RawGlitch.chain(bytes, size[0], size[1], 4095, 256, frame))
            }
        }
        org.junit.Assert.assertEquals(
            "775559dca379e62fd0748174b1ca28be4eff2ca47b0ff7dcfe3d6fdae88beb40",
            HexFormat.of().formatHex(frames.digest()),
        )
        org.junit.Assert.assertEquals(
            "bf8a161232e661899646e6a282dc77a73d72dae59c800afe1f5de067f2c0581e",
            HexFormat.of().formatHex(raw.digest()),
        )
    }
}
