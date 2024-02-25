package com.gerbort.scanner

sealed interface ScannerUiEvents {
    data object InitSuccess: ScannerUiEvents
    data object InitFailed : ScannerUiEvents
    data object EntryAlreadyExists: ScannerUiEvents
    data object EntryCreated: ScannerUiEvents
}