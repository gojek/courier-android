package com.gojek.chuckmqtt.internal.presentation.servicelocator

import androidx.lifecycle.ViewModel
import com.gojek.chuckmqtt.internal.MqttChuck.mqttChuckUseCase
import com.gojek.chuckmqtt.internal.presentation.base.MqttChuckViewModelFactory
import com.gojek.chuckmqtt.internal.presentation.base.Provider
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.actionprocessor.DefaultTransactionDetailActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailFragmentViewModel
import com.gojek.chuckmqtt.internal.presentation.transactionlist.actionprocessor.DefaultTransactionListActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel.TransactionListFragmentViewModel

internal object MqttChuckPresentationDependencyLocator {

    val viewModelFactory = MqttChuckViewModelFactory(
        mapOf(
            TransactionListFragmentViewModel::class.java to getTransactionListFragmentViewModelProvider(),
            TransactionDetailFragmentViewModel::class.java to getTransactionDetailFragmentViewModelProvider()

        )
    )

    private fun getTransactionListFragmentViewModelProvider(): Provider<ViewModel> {
        return object : Provider<ViewModel> {
            override fun get(): ViewModel {
                return TransactionListFragmentViewModel(
                    DefaultTransactionListActionProcessorProvider(mqttChuckUseCase())
                )
            }
        }
    }

    private fun getTransactionDetailFragmentViewModelProvider(): Provider<ViewModel> {
        return object : Provider<ViewModel> {
            override fun get(): ViewModel {
                return TransactionDetailFragmentViewModel(
                    DefaultTransactionDetailActionProcessorProvider(mqttChuckUseCase())
                )
            }
        }
    }
}
