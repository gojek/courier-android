package com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.presentation.theme.ChuckMqttTheme
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.GetTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.ShareTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewState
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel

@Composable
internal fun TransactionDetailScreen(
    transactionId: Long,
    intentLambda: (TransactionDetailIntent) -> Unit,
    transactionDetailViewModel: TransactionDetailViewModel
) {
    val state by transactionDetailViewModel.states()
        .subscribeAsState(TransactionDetailViewState.default())

    ChuckMqttTheme {
        Scaffold(
            topBar = {
                DetailAppBar(
                    toolBarTitle = state.transaction.packetName,
                    isSent = state.transaction.isSent,
                    onShareClick = {
                        intentLambda(ShareTransactionDetailIntent(transactionId))
                    },
                    onSearchClick = {
                    }
                )
            },
            content = {
                TransactionDetail(transaction = state.transaction)

                LaunchedEffect(Unit) {
                    intentLambda(
                        GetTransactionDetailIntent(
                            transactionId
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun TransactionDetail(transaction: MqttTransactionUiModel) {
    val packetInfo = HtmlCompat.fromHtml(
        transaction.packetInfo,
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )
    Column(modifier = Modifier.padding(16.dp)) {
        if (packetInfo.isNotEmpty()) {
            Text(
                text = packetInfo.toString(),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
            )
        }
        Text(
            text = transaction.packetBody,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
        )
    }
}

@Composable
internal fun DetailAppBar(
    toolBarTitle: String,
    isSent: Boolean,
    onShareClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = toolBarTitle)
        },
        actions = {
            val iconDrawable =
                if (isSent) {
                    R.drawable.mqtt_ic_message_sent
                } else {
                    R.drawable.mqtt_ic_message_received
                }
            Icon(
                painter = painterResource(id = iconDrawable),
                contentDescription = if (isSent) {
                    "Message Sent"
                } else {
                    "Message Received"
                }
            )

            IconButton(onClick = { onSearchClick() }) {
                Icon(
                    painter = painterResource(id = R.drawable.mqtt_chuck_ic_search_white_24dp),
                    contentDescription = "Search Menu Icon"
                )
            }

            IconButton(onClick = { onShareClick() }) {
                Icon(
                    painter = painterResource(id = R.drawable.mqtt_chuck_ic_share_white_24dp),
                    contentDescription = "Share Menu Icon"
                )
            }
        },
        backgroundColor = MaterialTheme.colors.primarySurface,
        elevation = 4.dp
    )
}
