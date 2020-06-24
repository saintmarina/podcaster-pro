package com.saintmarina.recordingsystem.googledrive

class FileSyncStatus private constructor (val message: String, val error: Boolean) {
    companion object {
        fun success(value: String): FileSyncStatus {
            return FileSyncStatus(
                value,
                false
            )
        }

        fun error(value: String): FileSyncStatus {
            return FileSyncStatus(
                value,
                true
            )
        }
    }
}