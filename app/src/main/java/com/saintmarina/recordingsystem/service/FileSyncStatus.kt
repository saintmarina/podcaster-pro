package com.saintmarina.recordingsystem.service

class FileSyncStatus (private val message: String = "", private val  error: Boolean = false) {
    fun getStatusMessage(): String {
        return message
    }

    fun errorOccurred(): Boolean {
        return error
    }

    companion object {
        fun success(value: String): FileSyncStatus {
            return FileSyncStatus(value, false)
        }

        fun error(value: String): FileSyncStatus {
            return FileSyncStatus(value, true)
        }
    }
}