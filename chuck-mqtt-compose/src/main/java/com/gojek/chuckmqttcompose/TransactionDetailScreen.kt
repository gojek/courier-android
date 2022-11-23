package com.gojek.chuckmqttcompose

import androidx.compose.foundation.Image
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailAppBar(
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
