package com.example.radek.jobexecutor

import com.example.radek.jobexecutor.response.InitialPagedResponse
import com.example.radek.jobexecutor.response.FollowingPagedResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never


@RunWith(MockitoJUnitRunner::class)
class PageProviderExecutorTest {


    @Mock
    private lateinit var pagedDataProvider: PagedDataProvider<String>
    @Mock
    private lateinit var observer: (State) -> Unit
    private lateinit var pageProviderExecutor: PageProviderExecutor<String>

    @Before
    fun init() {
        pageProviderExecutor = PageProviderExecutor()
        pageProviderExecutor.repositoryState = observer
        pageProviderExecutor.attachDataProvider(pagedDataProvider)
    }

    @Test
    fun `when job is started progress status is emitted`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        pageProviderExecutor.loadInitialPage(callback)

        Mockito.verify(observer)(State.Loading)
    }

    @Test
    fun `when job is successfully finished success status is emitted`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val result = InitialPagedResponse(10, listOf(""))
        val anyOfTypeSuccess = any<(InitialPagedResponse<String>) -> Unit>()
        Mockito.`when`(pagedDataProvider.provideInitialData(anyOfTypeSuccess, any())).doAnswer {
            val onSuccess = it.getArgument<(InitialPagedResponse<String>) -> Unit>(0)
            onSuccess(result)
        }
        pageProviderExecutor.loadInitialPage(callback)

        Mockito.verify(observer)(State.Loaded)
    }

    @Test
    fun `when job is failed and no jobs in progress failure status is emitted`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val anyOfTypeSuccess = any<(InitialPagedResponse<String>) -> Unit>()
        val anyOfTypeFailure = any<(Throwable) -> Unit>()
        val exception = Exception()
        Mockito.`when`(pagedDataProvider.provideInitialData(anyOfTypeSuccess, anyOfTypeFailure)).doAnswer {
            val onFailure = it.getArgument<(Throwable) -> Unit>(1)
            onFailure(exception)
        }
        pageProviderExecutor.loadInitialPage(callback)

        Mockito.verify(observer)(any<State.Failed>())
    }

    @Test
    fun `when job is failed but there are jobs in progress failure status is not emitted`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val errorCaptor = argumentCaptor<(Throwable) -> Unit>()
        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(any(),errorCaptor.capture())
        errorCaptor.lastValue.invoke(Exception())

        Mockito.verify(observer, never())(any<State.Failed>())
    }

    @Test
    fun `when job is succeed but there are jobs in progress success status is not emitted`() {
        val result = InitialPagedResponse(10, listOf(""))
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val captor = argumentCaptor<(InitialPagedResponse<String>) -> Unit>()
        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(captor.capture(), any())
        captor.lastValue.invoke(result)
        Mockito.verify(observer, never())(any<State.Loaded>())
    }

    @Test
    fun `when there is only one job execute it and provide result`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val result = InitialPagedResponse(10, listOf(""))
        val captor = argumentCaptor<(InitialPagedResponse<String>) -> Unit>()

        pageProviderExecutor.loadInitialPage(callback)
        Mockito.verify(pagedDataProvider).provideInitialData(captor.capture(), any())
        captor.lastValue.invoke(result)
        Mockito.verify(callback).invoke(result)
    }

    @Test
    fun `when there is more than one job do not start it yet`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val captor = argumentCaptor<(InitialPagedResponse<String>) -> Unit>()
        val captor2 = argumentCaptor<(FollowingPagedResponse<String>) -> Unit>()

        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(captor.capture(), any())
        Mockito.verify(pagedDataProvider, never()).providePageData(eq(2), captor2.capture(), any())
    }

    @Test
    fun `second job is executed after first is succeed`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val result = InitialPagedResponse(10, listOf(""))
        val result2 = FollowingPagedResponse(10, 2, listOf(""))
        val captor = argumentCaptor<(InitialPagedResponse<String>) -> Unit>()
        val captor2 = argumentCaptor<(FollowingPagedResponse<String>) -> Unit>()

        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(captor.capture(), any())
        Mockito.verify(pagedDataProvider, never()).providePageData(eq(2), any(), any())

        captor.lastValue.invoke(result)
        Mockito.verify(pagedDataProvider).providePageData(eq(2), captor2.capture(), any())

        captor2.lastValue.invoke(result2)
        Mockito.verify(callback2).invoke(result2)
    }

    @Test
    fun `second job is executed after first is failed`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val result2 = FollowingPagedResponse(10, 2, listOf(""))

        val captor2 = argumentCaptor<(FollowingPagedResponse<String>) -> Unit>()
        val errorCaptor = argumentCaptor<(Throwable) -> Unit>()

        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(any(), errorCaptor.capture())
        Mockito.verify(pagedDataProvider, never()).providePageData(eq(2), any(), any())
        errorCaptor.lastValue.invoke(Exception())

        Mockito.verify(pagedDataProvider).providePageData(eq(2), captor2.capture(), any())

        captor2.lastValue.invoke(result2)
        Mockito.verify(callback2).invoke(result2)
    }

    @Test
    fun `first failed job is resumed after retry called`() {
        val callback = mock<(InitialPagedResponse<String>) -> Unit>()
        val callback2 = mock<(FollowingPagedResponse<String>) -> Unit>()
        val captor2 = argumentCaptor<(FollowingPagedResponse<String>) -> Unit>()
        val errorCaptor = argumentCaptor<(Throwable) -> Unit>()
        val errorCaptor2 = argumentCaptor<(Throwable) -> Unit>()

        pageProviderExecutor.loadInitialPage(callback)
        pageProviderExecutor.loadPage(2, callback2)
        Mockito.verify(pagedDataProvider).provideInitialData(any(), errorCaptor.capture())
        Mockito.verify(pagedDataProvider, never()).providePageData(eq(2), any(), any())
        errorCaptor.lastValue.invoke(Exception())

        Mockito.verify(pagedDataProvider).providePageData(eq(2), captor2.capture(), errorCaptor2.capture())
        errorCaptor2.lastValue.invoke(Exception())

        Mockito.clearInvocations(pagedDataProvider)
        pageProviderExecutor.retryFailedJobs()
        Mockito.verify(pagedDataProvider).provideInitialData(any(), any())
    }
}