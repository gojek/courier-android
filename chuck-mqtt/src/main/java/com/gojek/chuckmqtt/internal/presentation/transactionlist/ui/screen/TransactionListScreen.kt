package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.presentation.theme.ChuckMqttTheme
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.ClearTransactionHistoryIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.StartObservingAllTransactionsIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewState
import com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel.TransactionListViewModel

@Composable
internal fun TransactionListScreen(
    navController: NavController,
    toolbarSubtitle: CharSequence,
    viewModel: TransactionListViewModel
) {
    val state by viewModel.states().subscribeAsState(TransactionListViewState.default())

    ChuckMqttTheme {
        Scaffold(
            topBar = {
                ListAppBar(
                    toolbarSubtitle = toolbarSubtitle,
                    onClearButtonClicked = {
                        viewModel.dispatchIntent(ClearTransactionHistoryIntent)
                    }
                )
            },
            content = {
                TransactionList(
                    navController = navController,
                    transactionList = state.transactionList
                )

                LaunchedEffect(Unit) {
                    viewModel.dispatchIntent(StartObservingAllTransactionsIntent)
                }
            }
        )
    }
}

@Composable
internal fun ListAppBar(
    toolbarSubtitle: CharSequence,
    onClearButtonClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("MQTT Chuck")
                Text(
                    text = toolbarSubtitle.toString(),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
                )
            }
        },
        actions = {
            IconButton(onClick = { onClearButtonClicked() }) {
                Icon(
                    painter = painterResource(id = R.drawable.mqtt_chuck_ic_delete_white_24dp),
                    contentDescription = "Delete Packets"
                )
            }
        },
        backgroundColor = MaterialTheme.colors.primarySurface,
        elevation = 4.dp
    )
}

@Composable
internal fun TransactionList(
    navController: NavController,
    transactionList: List<MqttTransactionUiModel>
) {
    LazyColumn {
        items(transactionList) { item ->
            TransactionListItem(
                navController = navController,
                item = item
            )
            Divider(color = Color.LightGray)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun TransactionListItem(
    navController: NavController,
    item: MqttTransactionUiModel,
) {
    val iconDrawable = if (item.isSent) {
        R.drawable.mqtt_ic_message_sent
    } else {
        R.drawable.mqtt_ic_message_received
    }

    Surface(
        onClick = {
            navController.navigate("transactionDetailScreen/${item.id}")
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = iconDrawable),
                contentDescription = if (item.isSent) {
                    "Message Sent"
                } else {
                    "Message Received"
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                with(item) {
                    Text(
                        text = packetName,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )

                    Text(
                        text = packetInfo,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    )

                    Text(
                        transmissionTime,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
