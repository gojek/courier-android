package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.OpenTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.adapter.TransactionListAdapter.TransactionListViewHolder
import com.gojek.chuckmqtt.internal.utils.extensions.debouncedClicks
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.mqtt_chucker_list_item_transaction.view.chucker_size
import kotlinx.android.synthetic.main.mqtt_chucker_list_item_transaction.view.chucker_time_start
import kotlinx.android.synthetic.main.mqtt_chucker_list_item_transaction.view.mqtt_content
import kotlinx.android.synthetic.main.mqtt_chucker_list_item_transaction.view.packet_name
import kotlinx.android.synthetic.main.mqtt_chucker_list_item_transaction.view.send_receive_view

internal class TransactionListAdapter(
    private val context: Context,
    private val onIntent: (TransactionListIntent) -> Unit
) : RecyclerView.Adapter<TransactionListViewHolder>() {

    private var transactions: List<MqttTransactionUiModel> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mqtt_chucker_list_item_transaction, parent, false)
        return TransactionListViewHolder(itemView)
    }

    override fun getItemCount() = transactions.size

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {
        holder.disposable?.dispose()
        holder.disposable = holder.bind(transactions[position])
            .subscribe(
                { onIntent(it) },
                { Log.e("TransactionListAdapter", "exception : ", it) }
            )
    }

    inner class TransactionListViewHolder(itemView: View, var disposable: Disposable? = null) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(mqttTransactionUiModel: MqttTransactionUiModel): Observable<TransactionListIntent> {
            with(itemView) {
                with(mqttTransactionUiModel) {
                    packet_name.text = packetName
                    mqtt_content.text = packetPreview
                    chucker_time_start.text = transmissionTime
                    chucker_size.text = bytesString
                    if(isSent) {
                        send_receive_view.setImageResource(R.drawable.mqtt_ic_message_sent)
                    } else {
                        send_receive_view.setImageResource(R.drawable.mqtt_ic_message_received)
                    }
                }
            }
            return itemView.debouncedClicks().map {
                OpenTransactionDetailIntent(mqttTransactionUiModel.id)
            }
        }
    }

    fun setData(transactionListNew: List<MqttTransactionUiModel>) {
        val previousList = transactions
        this.transactions = transactionListNew

        DiffUtil.calculateDiff(GenericDiffUtilCallback(
            oldList = previousList,
            newList = transactionListNew,
            itemComparator = { old, new ->
                old == new
            },
            itemContentComparator = { old, new ->
                old == new
            }
        )).dispatchUpdatesTo(this)
    }
}