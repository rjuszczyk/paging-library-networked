package com.example.radek.jobexecutor

sealed class State {
    object NotStarted : State()
    object Loading : State()
    object Loaded : State()
    class Failed(val cause:Throwable) : State()
}
