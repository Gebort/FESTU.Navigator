package com.example.festunavigator.presentation.preview.navigation

import androidx.navigation.NavController
import com.gerbort.app.R
import com.gerbort.initialization.navigation.OrientationNavigator
import com.gerbort.router.navigation.RouterNavigator
import com.gerbort.scanner.navigation.ScannerNavigator
import com.gerbort.scanner.scanner.ScannerFragment
import com.gerbort.search.SearchFragment
import com.gerbort.search.navigation.SearchNavigator

class AppNavigator:
    OrientationNavigator,
    RouterNavigator,
    ScannerNavigator,
    SearchNavigator {

    private var navController: NavController? = null

    fun bind(navController: NavController) {
        this.navController = navController
    }

    fun unbind() {
        navController = null
    }

    override fun navigateOrientationToScanner() {
        navController?.navigate(
            R.id.action_orientationFragment_to_scannerFragment,
            ScannerFragment.createBundleInitialize()
        )
    }

    override fun navigateRouterToEntryCreation() {
        navController?.navigate(
            R.id.action_global_scannerFragment,
            ScannerFragment.createBundleEntryCreation()
        )
    }

    override fun navigateRouterToSearch(startLocation: Boolean) {
        navController?.navigate(
            R.id.action_routerFragment_to_searchFragment,
            if (startLocation) SearchFragment.createBundleStart() else SearchFragment.createBundleEnd()
        )
    }

    override fun popBackStack() {
        navController?.popBackStack()
    }

    override fun navigateOnEntryCreationSuccess() {
        navController?.navigate(
            R.id.action_confirmFragment_to_routerFragment
        )
    }

    override fun navigateFromScannerToConfirmer() {
        navController?.navigate(
            R.id.action_scannerFragment_to_confirmFragment
        )
    }
}