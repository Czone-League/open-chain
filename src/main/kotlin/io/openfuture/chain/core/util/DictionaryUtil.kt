package io.openfuture.chain.core.util

import io.openfuture.chain.core.model.entity.base.Dictionary

object DictionaryUtil {

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<*>> valueOf(clazz: Class<out T>, id: Int): T {
        val values = clazz.enumConstants
        return values.firstOrNull { (it as Dictionary).getId() == id }
            ?: throw IllegalStateException("Type ID not found")
    }

}