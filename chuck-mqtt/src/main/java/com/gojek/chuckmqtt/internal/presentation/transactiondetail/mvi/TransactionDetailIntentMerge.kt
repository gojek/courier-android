package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import io.reactivex.Observable

internal fun intentMerge(shared: Observable<TransactionDetailIntent>): Observable<TransactionDetailIntent> {
    return Observable.merge(
        listOf(
            shared.ofType(TransactionDetailIntent.GetTransactionDetailIntent::class.java),
            shared.ofType(TransactionDetailIntent.ShareTransactionDetailIntent::class.java)
        )
    )
}