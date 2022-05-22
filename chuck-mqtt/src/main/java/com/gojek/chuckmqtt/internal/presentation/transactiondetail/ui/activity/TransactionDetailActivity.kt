package com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.base.activity.BaseChuckMqttActivity
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.fragment.TransactionDetailFragment
import kotlinx.android.synthetic.main.activity_transaction_list.toolbar

internal class TransactionDetailActivity : BaseChuckMqttActivity() {

    private val FRAGMENT_TAG = "transaction_detail_frag"

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        setSupportActionBar(toolbar)

        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0)
        addTransactionDetailFragment(TransactionDetailFragment.newInstance(transactionId))
    }

    private fun addTransactionDetailFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.header_fragment_container, fragment, FRAGMENT_TAG)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commitAllowingStateLoss()
    }

    companion object {
        private const val EXTRA_TRANSACTION_ID = "transaction_id"

        fun startTransactionDetailActivity(context: Context, transactionId: Long) {
            val intent = Intent(context, TransactionDetailActivity::class.java)
            intent.putExtra(EXTRA_TRANSACTION_ID, transactionId)
            context.startActivity(intent)
        }
    }
}
