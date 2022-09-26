package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.base.fragment.FoodMviBaseFragment
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.activity.TransactionDetailActivity
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.ClearTransactionHistoryIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.StartObservingAllTransactionsIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewState
import com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.adapter.TransactionListAdapter
import com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel.TransactionListViewModel
import com.gojek.chuckmqtt.internal.utils.extensions.hide
import com.gojek.chuckmqtt.internal.utils.extensions.ifTrue
import com.gojek.chuckmqtt.internal.utils.extensions.show
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_transaction_list.transaction_list
import kotlinx.android.synthetic.main.fragment_transaction_list.transaction_list_loader
import kotlin.reflect.KClass

internal class TransactionListFragment :
    FoodMviBaseFragment<TransactionListIntent, TransactionListViewState, TransactionListViewModel>() {

    private lateinit var transactionListAdapter: TransactionListAdapter

    override val clazz: KClass<TransactionListViewModel>
        get() = TransactionListViewModel::class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupObserver()
        _intents.onNext(StartObservingAllTransactionsIntent)
    }

    override fun onDestroyView() {
        compositeBag.clear()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.mqtt_transactions_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.clear) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.mqtt_chuck_clear)
                .setMessage(R.string.mqtt_chuck_clear_mqtt_confirmation)
                .setPositiveButton(
                    R.string.mqtt_chuck_clear
                ) { _, _ ->
                    _intents.onNext(ClearTransactionHistoryIntent)
                }
                .setNegativeButton(R.string.mqtt_chuck_cancel, null)
                .show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun initView() {
        transaction_list.layoutManager = LinearLayoutManager(requireContext())
        transaction_list.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        transactionListAdapter = TransactionListAdapter(requireContext()) {
            _intents.onNext(it)
        }
        transaction_list.adapter = transactionListAdapter
    }

    private fun setupObserver() {
        compositeBag += vm.states().subscribe(this::render)
        compositeBag += vm.effects().subscribe(this::handleViewEffects)
        vm.processIntents(intents())
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionListFragment()
    }

    override fun intents(): Observable<TransactionListIntent> {
        return intents
    }

    override fun render(state: TransactionListViewState) {
        with(state) {
            ifTrue(showLoadingView) {
                transaction_list_loader.show()
            }

            transaction_list_loader.hide()
            transactionListAdapter.setData(transactionList)
        }
    }

    private fun handleViewEffects(effect: TransactionListViewEffect) {
        when (effect) {
            is TransactionListViewEffect.OpenTransactionDetailViewEffect -> {
                openTransactionDetail(effect.transactionId)
            }
        }
    }

    private fun openTransactionDetail(transactionId: Long) {
        TransactionDetailActivity.startTransactionDetailActivity(requireContext(), transactionId)
    }
}