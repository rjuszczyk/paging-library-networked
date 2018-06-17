package com.example.radek.android_viewmodel_datastate

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class MainViewModel (
        private val mainStringRepository: MainStringRepository,
        restoredState: MainViewDataState?
    ) : ViewModel() {

    internal var myString = MutableLiveData<String>()
    internal var state: MutableLiveData<MainViewDataState>

    init {
        log("MainViewModel init")
        state = MutableLiveData()
        if(restoredState == null) {
            state.value = MainViewDataState(MainViewDataState.NOT_STARTED)
        } else {
            state.value = restoredState
            if(restoredState.state == MainViewDataState.LOADING || restoredState.state == MainViewDataState.LOADED) {
                performLoading()
            }
        }
    }

    fun startLoading() {
        state.postValue(MainViewDataState(MainViewDataState.LOADING))
        performLoading()
    }

    private fun performLoading() {
        log("performLoading")
        mainStringRepository.loadString(object : MainStringRepository.StringCallback {
            override fun loaded(loadedString: String) {
                state.postValue(MainViewDataState(MainViewDataState.LOADED))
                myString.postValue(loadedString)
            }

            override fun failed(errorMessage: String) {
                state.postValue(MainViewDataState(MainViewDataState.FAILED))
            }
        })
    }
}
