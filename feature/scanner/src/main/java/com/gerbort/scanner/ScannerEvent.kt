package com.gerbort.scanner

sealed interface ScannerEvent {

    class NewScanType(val scanType: ConfirmType): ScannerEvent
    class NewConfirmationObject(val confObject: LabelObject): ScannerEvent
    data object AcceptObject : ScannerEvent
    data object RejectObject : ScannerEvent

}