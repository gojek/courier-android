package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.base.activity.BaseChuckMqttActivity
import com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.fragment.TransactionListComposeFragment

internal class TransactionListComposeActivity : BaseChuckMqttActivity() {

    private val FRAGMENT_TAG = "transaction_list_frag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_list_2)

        addTransactionListFragment(TransactionListComposeFragment.newInstance())
    }

    private fun addTransactionListFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.header_fragment_container, fragment, FRAGMENT_TAG)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commitAllowingStateLoss()
    }
}
