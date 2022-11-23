package com.gojek.chuckmqttcompose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ListAppBar(
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
