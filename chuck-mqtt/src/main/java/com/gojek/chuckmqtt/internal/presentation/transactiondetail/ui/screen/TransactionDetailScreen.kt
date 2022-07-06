package com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.screen

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.presentation.theme.ChuckMqttTheme
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.GetTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.ShareTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewState
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel
import com.gojek.chuckmqtt.internal.utils.indexesOf

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

@Composable
internal fun DetailAppBar(
    toolBarTitle: String,
    isSent: Boolean,
    onShareClick: () -> Unit,
    onQueryTextChange: (String) -> Unit,
    onClearSearchQueryClicked: () -> Unit
) {
    var shouldShowSearchBox by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (shouldShowSearchBox) {
                var text by remember { mutableStateOf(TextFieldValue("")) }

                TextField(
                    value = text,
                    onValueChange = {
                        onQueryTextChange(it.text)
                        text = it
                    },
                    placeholder = { Text("Search...") },
                    textStyle = TextStyle(fontSize = 16.sp, color = Color.Gray),
                    colors = TextFieldDefaults.textFieldColors(
                        placeholderColor = Color.Gray,
                        backgroundColor = MaterialTheme.colors.primarySurface
                    )
                )
            } else {
                Text(text = toolBarTitle)
            }
        },
        actions = {
            if (!shouldShowSearchBox) {
                val iconDrawable =
                    if (isSent) {
                        R.drawable.mqtt_ic_message_sent
                    } else {
                        R.drawable.mqtt_ic_message_received
                    }

                Image(
                    painter = painterResource(id = iconDrawable),
                    contentDescription = if (isSent) {
                        "Message Sent"
                    } else {
                        "Message Received"
                    }
                )
            }

            if (toolBarTitle == "PUBLISH") {
                IconButton(onClick = {
                    shouldShowSearchBox = !shouldShowSearchBox

                    if (!shouldShowSearchBox) {
                        onClearSearchQueryClicked()
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (!shouldShowSearchBox) {
                                R.drawable.mqtt_chuck_ic_search_white_24dp
                            } else {
                                R.drawable.mqtt_chuck_ic_delete_white_24dp
                            }
                        ),
                        contentDescription = "Search Menu Icon"
                    )
                }
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

@Preview
@Composable
fun PreviewDetailAppBar() {
    DetailAppBar(
        toolBarTitle = "Title",
        isSent = false,
        onShareClick = {},
        onQueryTextChange = {},
        onClearSearchQueryClicked = {})
}
