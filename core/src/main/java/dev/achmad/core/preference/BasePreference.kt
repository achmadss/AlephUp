package dev.achmad.core.preference

open class BasePreference {
    fun getPrefKey(key: String) =
        this::class.java.`package`?.name + "_" + this::class.java.simpleName + "_" + key
}