package com.skyd.anivu.ui.fragment.settings.data

import com.skyd.anivu.base.mvi.MviViewState

data class DataState(
    val dataListState: DataListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = DataState(
            dataListState = DataListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface DataListState {
    data object Init : DataListState
}