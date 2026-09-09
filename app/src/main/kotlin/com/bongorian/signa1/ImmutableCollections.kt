package com.bongorian.signa1

import java.util.Collections

/** Snapshot copies also reject mutation through Java or a Kotlin cast. */
internal fun <K, V> Map<K, V>.immutableCopy(): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(this))
