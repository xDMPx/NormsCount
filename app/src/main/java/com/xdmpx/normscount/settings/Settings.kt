package com.xdmpx.normscount.settings

abstract class Settings {

    companion object {
        @Volatile
        private var INSTANCE: SettingsInstance? = null

        fun getInstance(): SettingsInstance {
            synchronized(this) {
                return INSTANCE ?: SettingsInstance(
                ).also {
                    INSTANCE = it
                }
            }
        }
    }
}