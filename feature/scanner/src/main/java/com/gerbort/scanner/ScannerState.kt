package com.gerbort.scanner

data class ScannerState(
    val confirmObject: LabelObject? = null,
    val confirmType: ConfirmType = ConfirmType.INITIALIZE
)

enum class ConfirmType {
    INITIALIZE,
    ENTRY,
}