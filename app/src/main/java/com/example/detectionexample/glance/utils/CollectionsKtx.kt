package com.example.detectionexample.glance.utils

fun <T> MutableList<T>.removeIf(filter: (T) -> Boolean): Boolean {
    var removed = false
    val toRemove = mutableListOf<T>()

    forEach { item ->
        if (filter(item)) {
            removed = true
            toRemove.add(item)
        }
    }

    removeAll(toRemove)
    return removed
}

fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, calculation: (K) -> V): V {
    return this.getOrPut(key) { calculation(key) }
}
