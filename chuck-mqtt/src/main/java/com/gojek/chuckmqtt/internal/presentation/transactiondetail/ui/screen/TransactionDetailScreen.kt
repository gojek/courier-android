package com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.screen

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.GetTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.ShareTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewState
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel
import com.gojek.chuckmqtt.internal.utils.indexesOf
import com.gojek.chuckmqttcompose.DetailAppBar
import com.gojek.chuckmqttcompose.theme.ChuckMqttTheme

@Composable
internal fun TransactionDetailScreen(
    transactionId: Long,
    transactionDetailViewModel: TransactionDetailViewModel
) {
    val state by transactionDetailViewModel.states()
        .subscribeAsState(TransactionDetailViewState.default())

    var queriedPacketBody by remember {
        mutableStateOf("")
    }

    transactionDetailViewModel.dispatchIntent(
        GetTransactionDetailIntent(
            transactionId
        )
    )

    ChuckMqttTheme {
        Scaffold(
            topBar = {
                DetailAppBar(
                    toolBarTitle = state.transaction.packetName,
                    isSent = state.transaction.isSent,
                    onShareClick = {
                        transactionDetailViewModel.dispatchIntent(
                            ShareTransactionDetailIntent(
                                transactionId
                            )
                        )
                    },
                    onQueryTextChange = {
                        queriedPacketBody = it
                    },
                    onClearSearchQueryClicked = {
                        queriedPacketBody = ""
                    }
                )
            },
            content = {
                TransactionDetail(
                    transaction = state.transaction,
                    queriedPackedBody = queriedPacketBody
                )
            }
        )
    }
}

@Composable
private fun TransactionDetail(transaction: MqttTransactionUiModel, queriedPackedBody: String) {
    val packetInfo = HtmlCompat.fromHtml(
        transaction.packetInfo,
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )
    Column(modifier = Modifier.padding(16.dp)) {
        if (packetInfo.isNotEmpty()) {
            AndroidView(factory = { context -> TextView(context) }, update = {
                it.text = packetInfo
            })
        }
        Text(
            text = if (queriedPackedBody.isNotBlank()) {
                val startIndexes = indexesOf(transaction.packetBody, queriedPackedBody)
                val length = queriedPackedBody.length

                startIndexes.fold(AnnotatedString.Builder(transaction.packetBody)) { builder, position ->
                    builder.addStyle(
                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                        position,
                        position + length
                    )
                    builder.addStyle(
                        style = SpanStyle(color = Color.Red),
                        position,
                        position + length
                    )
                    builder.addStyle(
                        style = SpanStyle(background = Color.Yellow),
                        position,
                        position + length
                    )
                    builder
                }.toAnnotatedString()
            } else {
                buildAnnotatedString {
                    append(transaction.packetBody)
                }
            },
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
        )
    }
}
