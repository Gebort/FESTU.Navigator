package com.example.festunavigator.presentation.common.helpers

import com.example.festunavigator.databinding.FragmentPreviewBinding
import com.example.festunavigator.presentation.preview.PreviewFragment

class ViewStateHelper(
    private val fragment: PreviewFragment,
    private val binding: FragmentPreviewBinding
) {

//    fun onStartingStart(){
//        with(fragment){
//            selectNode(null)
//            preload()
//            binding.confirmLayout.doOnLayout { animationHelper.placeViewOut(app.view.confirmLayout) }
//            binding.routeLayout.doOnLayout { animationHelper.placeViewOut(view.routeLayout) }
//            binding.routeBigLayout.doOnLayout { animationHelper.placeViewOut(view.routeBigLayout) }
//            binding.entryRecyclerView.adapter = adapter
//            binding.entryRecyclerView.layoutManager = LinearLayoutManager(
//                fragment.requireActivity().applicationContext
//            )
//        }
//
//    }
//
//    fun onInitializeStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.fadeShow(binding.bordersImage)
//                it.fadeShow(binding.scanText)
//            }
//        }
//    }
//
//    fun onInitializeEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.fadeHide(binding.bordersImage)
//                it.fadeHide(binding.scanText)
//            }
//        }
//    }
//
//    fun onInitializeConfirmStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewUp(view.confirmLayout)
//            }
//        }
//    }
//
//    fun onInitializeConfirmEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewDown(view.confirmLayout)
//            }
//        }
//    }
//
//    fun onEntryCreationStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.fadeShow(binding.bordersImage)
//                it.fadeShow(binding.scanText)
//            }
//        }
//    }
//
//    fun onEntryCreationEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.fadeHide(binding.bordersImage)
//                it.fadeHide(binding.scanText)
//            }
//        }
//    }
//
//    fun onEntryConfirmStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewUp(view.confirmLayout)
//            }
//        }
//    }
//
//    fun onEntryConfirmEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewDown(view.confirmLayout)
//            }
//        }
//    }
//
//    fun onGoingStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewUp(view.routeLayout)
//            }
//        }
//    }
//
//    fun onGoingEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewDown(view.routeLayout)
//            }
//        }
//    }
//
//    fun onChoosingStart() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewUp(view.routeBigLayout, false){
//                    animationHelper.viewRequestInput(
//                        binding.searchInput,
//                        view.activity.applicationContext
//                    )
//                }
//            }
//        }
//    }
//
//    fun onChoosingEnd() {
//        with (fragment) {
//            animationHelper.let {
//                it.slideViewDown(view.routeBigLayout, false)
//                it.viewHideInput(view.searchInput, view.activity.applicationContext)
//            }
//        }
//    }
}