package com.bongorian.signa1

/** Reads the existing human-readable photo description; never guesses a partial preset. */
internal data class SavedSignal(val description: String, val chain: String, val state: EffectState?,
                                val experimental: Boolean?) {
    companion object {
        fun read(description: String?): SavedSignal? {
            if (description.isNullOrBlank() || description.length > 65536) return null
            val parts = description.split(" | ")
            if (parts.size < 3 || !parts[0].startsWith("5igna1 ")) return null
            // Framework EXIF writes these descriptions as ASCII, replacing the route arrow.
            val chain = parts[1].replace(" ? "," → ")
            val parsed = runCatching {
                val ids = if (chain == "CLEAN") emptyList() else chain.split(" → ").map { name ->
                    Effects.NAMES.indexOf(name).also { require(it > 0) }
                }
                require(ids.distinct().size == ids.size && ids == ids.sortedBy { Effects.rank(it) })
                val level = Regex("(?:^| )LEVEL=([^ ]+)").find(parts[2])!!.groupValues[1].toFloat()
                require(level.isFinite() && level in 0f..1f)
                var parameters = EffectParameters.defaults()
                for (id in ids) {
                    val prefix = Effects.name(id) + " identity="
                    val group = parts.filter { it.startsWith(prefix) }.single()
                    val tokens = group.removePrefix(Effects.name(id)+" ").substringBefore(" overrides=")
                        .split(' ').filter { it.isNotBlank() }.map {
                            val pair = it.split('=',limit=2);require(pair.size==2);pair[0] to pair[1]
                        }
                    require(tokens.map { it.first }.distinct().size == tokens.size)
                    val fields = tokens.toMap()
                    parameters = parameters.reseed(id,fields.getValue("identity").toLong())
                    for ((key,value) in fields) when (key) {
                        "identity" -> Unit
                        "eventIdentity" -> parameters = parameters.withEventIdentity(id,value.toLong())
                        "experimental" -> require(value == "true" || value == "false")
                        "input" -> require(value == "TAP@READOUT/DATA")
                        else -> {
                            val number = value.toFloat()
                            require(number.isFinite() && number in 0f..1f)
                            parameters = parameters.with(id,key,number)
                        }
                    }
                    // Older descriptions intentionally omitted these default transport macros.
                    for (control in Effects.CONTROLS[id]) require(control.key in fields ||
                        control.key in setOf("transport","reduce","cable","upconvert"))
                    if (" overrides=" in group) {
                        val tail = group.substringAfter(" overrides=")
                        require(tail.startsWith('{') && '}' in tail)
                        val body = tail.substringAfter('{').substringBefore('}')
                        val seen = HashSet<String>()
                        for (entry in body.split(", ").filter { it.isNotBlank() }) {
                            val pair=entry.split('=',limit=2);require(pair.size==2 && seen.add(pair[0]))
                            val value=pair[1].toFloat();require(value.isFinite())
                            parameters=parameters.override(id,pair[0],value)
                        }
                        require(tail.substringAfter('}').trim().let { it.isEmpty() || it.matches(Regex("(?:experimental=(?:true|false))?(?: ?input=TAP@READOUT/DATA)?")) })
                    }
                }
                EffectState.defaults().edit(ids.size>1,ids.fold(0) { mask,id -> mask or (1 shl id) },parameters).amount(level)
            }.getOrNull()
            val experimental = Regex("(?:^| )experimental=(true|false)(?: |$)").find(description)?.groupValues?.get(1)?.toBoolean()
            return SavedSignal(description,chain,parsed,experimental)
        }
    }
}
