package com.gerbort.scanner

import com.gerbort.common.model.TreeNode
import java.lang.Exception

sealed interface ScannerUiEvents {
    class InitSuccess(val entry: String?): ScannerUiEvents
    data object InitFailed : ScannerUiEvents
    data object EntryAlreadyExists: ScannerUiEvents
    data object EntryCreated: ScannerUiEvents
}