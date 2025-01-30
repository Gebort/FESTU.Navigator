package com.gerbort.scanner.navigation

interface ScannerNavigator {

    fun popBackStack()

    fun navigateOnEntryCreationSuccess()

    fun navigateFromScannerToConfirmer()

}