package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.adapter

import androidx.recyclerview.widget.DiffUtil

internal class GenericDiffUtilCallback<T>(
    private val oldList: List<T>,
    private val newList: List<T>,
    private val itemComparator: ((T, T) -> Boolean)? = null,
    private val itemContentComparator: ((T, T) -> Boolean)? = null
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        (
            itemComparator
                ?: itemContentComparator
            )?.invoke(oldList[oldItemPosition], newList[newItemPosition])
            ?: false

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        itemContentComparator?.invoke(oldList[oldItemPosition], newList[newItemPosition])
            ?: false

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
}
