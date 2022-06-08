package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.ClearTransactionHistoryIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.OpenTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.StartObservingAllTransactionsIntent
import io.reactivex.Observable

internal fun intentMerge(shared: Observable<TransactionListIntent>): Observable<TransactionListIntent> {
    return Observable.merge(
        listOf(
            shared.ofType(StartObservingAllTransactionsIntent::class.java),
            shared.ofType(OpenTransactionDetailIntent::class.java),
            shared.ofType(ClearTransactionHistoryIntent::class.java)
        )
    )
}
