package com.gojek.keepalive

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OptimalKeepAliveFailureHandlerTest {
    private val optimalKeepAliveProvider = mock<OptimalKeepAliveProvider>()

    private val optimalKeepAliveFailureHandler = OptimalKeepAliveFailureHandler(optimalKeepAliveProvider)

    @Test
    fun `test handleKeepAliveFailure`() {
        optimalKeepAliveFailureHandler.handleKeepAliveFailure()

        verify(optimalKeepAliveProvider).onOptimalKeepAliveFailure()
    }
}
