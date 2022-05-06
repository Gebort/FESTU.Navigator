package com.example.festunavigator.presentation.common.helpers

import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.festunavigator.presentation.AppRenderer

class ViewStateHelper(
    private val app: AppRenderer
) {

    fun onStartingStart(){
        with(app){
            selectNode(null)
            preload()
            view.confirmLayout.doOnLayout { animationHelper.placeViewOut(app.view.confirmLayout) }
            view.routeLayout.doOnLayout { animationHelper.placeViewOut(view.routeLayout) }
            view.routeBigLayout.doOnLayout { animationHelper.placeViewOut(view.routeBigLayout) }
            view.entryRecyclerView.adapter = adapter
            view.entryRecyclerView.layoutManager = LinearLayoutManager(
                view.activity.applicationContext
            )
        }

    }

    fun onInitializeStart() {
        with (app) {
            animationHelper.let {
                it.fadeShow(view.scanImage)
                it.fadeShow(view.scanText)
            }
        }
    }

    fun onInitializeEnd() {
        with (app) {
            animationHelper.let {
                it.fadeHide(view.scanImage)
                it.fadeHide(view.scanText)
            }
        }
    }

    fun onInitializeConfirmStart() {
        with (app) {
            animationHelper.let {
                it.slideViewUp(view.confirmLayout)
            }
        }
    }

    fun onInitializeConfirmEnd() {
        with (app) {
            animationHelper.let {
                it.slideViewDown(view.confirmLayout)
            }
        }
    }

    fun onEntryCreationStart() {
        with (app) {
            animationHelper.let {
                it.fadeShow(view.scanImage)
                it.fadeShow(view.scanText)
            }
        }
    }

    fun onEntryCreationEnd() {
        with (app) {
            animationHelper.let {
                it.fadeHide(view.scanImage)
                it.fadeHide(view.scanText)
            }
        }
    }

    fun onEntryConfirmStart() {
        with (app) {
            animationHelper.let {
                it.slideViewUp(view.confirmLayout)
            }
        }
    }

    fun onEntryConfirmEnd() {
        with (app) {
            animationHelper.let {
                it.slideViewDown(view.confirmLayout)
            }
        }
    }

    fun onGoingStart() {
        with (app) {
            animationHelper.let {
                it.slideViewUp(view.routeLayout)
            }
        }
    }

    fun onGoingEnd() {
        with (app) {
            animationHelper.let {
                it.slideViewDown(view.routeLayout)
            }
        }
    }

    fun onChoosingStart() {
        with (app) {
            animationHelper.let {
                it.slideViewUp(view.routeBigLayout, false){
                    animationHelper.viewRequestInput(
                        view.searchInput,
                        view.activity.applicationContext
                    )
                }
            }
        }
    }

    fun onChoosingEnd() {
        with (app) {
            animationHelper.let {
                it.slideViewDown(view.routeBigLayout, false)
                it.viewHideInput(view.searchInput, view.activity.applicationContext)
            }
        }
    }
}