package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class SavedSignalTest {
    private fun description(state: EffectState, experimental: Boolean = false, tap: Boolean = false): String {
        val frame = EffectState.Frame(state.ids(),state.amount,state.parameters(),123,2.5,emptyList(),experimental,tap)
        return "5igna1 1.6.1 | ${Effects.chainName(frame.ids())} | ${frame.describe()} | Processed RGB capture"
    }
    @Test fun restoresEveryStageIncludingOverridesAndSeeds() {
        var p=EffectParameters.defaults()
        for (id in Effects.ORDER.filter { it!=0 }) {
            p=p.reseed(id,Long.MIN_VALUE+id)
            for (c in Effects.CONTROLS[id]) p=p.with(id,c.key,.37f)
            p=p.override(id,"phase",-1.25f)
            if (FaultParameters.incidents(id)) p=p.withEventIdentity(id,987654321L)
        }
        val all=EffectState.defaults().chain(Effects.ORDER.fold(0) { mask,id -> if(id==0) mask else mask or (1 shl id) }).amount(.73f)
            .edit(true,Effects.ORDER.fold(0) { mask,id -> if(id==0) mask else mask or (1 shl id) },p)
        val saved=SavedSignal.read(description(all,true,true))!!
        assertEquals(all.encode(),saved.state!!.encode())
        assertEquals(all.encode(),SavedSignal.read(description(all,true,true).replace(" → "," ? "))!!.state!!.encode())
        assertEquals(true,saved.experimental)
    }
    @Test fun handlesCleanLegacyTransportDefaultsAndSingleFault() {
        for (state in listOf(EffectState.defaults().amount(0f),EffectState.defaults().single(Effects.CRT),EffectState.defaults().single(Effects.BIT_ERROR))) {
            val saved=SavedSignal.read(description(state))!!
            assertEquals(state.encode(),saved.state!!.encode())
        }
    }
    @Test fun rejectsIncompleteUnknownAndNonFiniteMetadataWithoutGuessing() {
        val source=description(EffectState.defaults().single(Effects.BIT_ERROR))
        assertNull(SavedSignal.read(null))
        assertNull(SavedSignal.read("another app | CLEAN | LEVEL=0.5"))
        assertNull(SavedSignal.read(source.replace("LEVEL=0.55","LEVEL=NaN"))!!.state)
        assertNull(SavedSignal.read(source.replace("BIT ERROR","FUTURE FAULT"))!!.state)
        assertNull(SavedSignal.read(source.replace(Regex("identity=[^ ]+"),"identity=broken"))!!.state)
        assertNull(SavedSignal.read("5igna1 1.0.0 | RAW ORIGINAL | Separate RAW exposure")!!.state)
        assertNull(SavedSignal.read(source.replace(Regex(" [a-zA-Z]+=[0-9.]+(?= |$)"),""))!!.state)
    }
}
