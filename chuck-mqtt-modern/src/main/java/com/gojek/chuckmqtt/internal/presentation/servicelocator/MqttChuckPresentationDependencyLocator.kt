package com.gojek.chuckmqtt.internal.presentation.servicelocator

import androidx.lifecycle.ViewModel
import com.gojek.chuckmqtt.internal.MqttChuck.mqttChuckUseCase
import com.gojek.chuckmqtt.internal.presentation.base.MqttChuckViewModelFactory
import com.gojek.chuckmqtt.internal.presentation.base.Provider
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.actionprocessor.DefaultTransactionDetailActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel
import com.gojek.chuckmqtt.internal.presentation.transactionlist.actionprocessor.DefaultTransactionListActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel.TransactionListViewModel

internal object MqttChuckPresentationDependencyLocator {

    val viewModelFactory = MqttChuckViewModelFactory(
        mapOf(
            TransactionListViewModel::class.java to getTransactionListFragmentViewModelProvider(),
            TransactionDetailViewModel::class.java to getTransactionDetailFragmentViewModelProvider()

        )
    )

    private fun getTransactionListFragmentViewModelProvider(): Provider<ViewModel> {
        return object : Provider<ViewModel> {
            override fun get(): ViewModel {
                return TransactionListViewModel(
                    DefaultTransactionListActionProcessorProvider(mqttChuckUseCase())
                )
            }
        }
    }

    private fun getTransactionDetailFragmentViewModelProvider(): Provider<ViewModel> {
        return object : Provider<ViewModel> {
            override fun get(): ViewModel {
                return TransactionDetailViewModel(
                    DefaultTransactionDetailActionProcessorProvider(mqttChuckUseCase())
                )
            }
        }
    }
}
