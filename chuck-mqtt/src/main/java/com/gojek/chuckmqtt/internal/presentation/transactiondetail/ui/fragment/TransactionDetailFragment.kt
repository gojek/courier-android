package com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ShareCompat
import androidx.core.text.HtmlCompat
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.base.fragment.FoodMviBaseFragment
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.GetTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewState
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.screen.TransactionDetailScreen
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel
import com.gojek.chuckmqtt.internal.utils.extensions.hide
import com.gojek.chuckmqtt.internal.utils.extensions.show
import com.gojek.chuckmqtt.internal.utils.highlightWithDefinedColors
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlin.reflect.KClass
import kotlinx.android.synthetic.main.fragment_transaction_detail.copy
import kotlinx.android.synthetic.main.fragment_transaction_detail.packet_body
import kotlinx.android.synthetic.main.fragment_transaction_detail.packet_info
import kotlinx.android.synthetic.main.fragment_transaction_detail.transaction_detail_loader

internal class TransactionDetailFragment :
    FoodMviBaseFragment<TransactionDetailIntent, TransactionDetailViewState, TransactionDetailViewModel>(),
    SearchView.OnQueryTextListener {

    private var backgroundSpanColor: Int = Color.YELLOW
    private var foregroundSpanColor: Int = Color.RED

    private var searchMenuItemVisible = false
    private var originalBody: String? = null

    private var transactionId = -1L

    override val clazz: KClass<TransactionDetailViewModel>
        get() = TransactionDetailViewModel::class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        transactionId = arguments?.getLong(EXTRA_TRANSACTION_ID, 0) ?: 0
        return ComposeView(requireContext()).apply {
            setContent {
                TransactionDetailScreen(
                    transactionId = transactionId,
                    intentLambda = intentLambda,
                    transactionDetailViewModel = vm
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()

        transactionId = arguments?.getLong(EXTRA_TRANSACTION_ID, 0) ?: 0
        _intents.onNext(GetTransactionDetailIntent(transactionId))
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.mqtt_chuck_transaction, menu)
//        val searchMenuItem = menu.findItem(R.id.mqtt_search)
//        if (searchMenuItemVisible) {
//            searchMenuItem.isVisible = true
//
//            val searchView = searchMenuItem.actionView as SearchView
//            searchView.setOnQueryTextListener(this)
//            searchView.setIconifiedByDefault(true)
//        }
//        return super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
//        R.id.mqtt_share -> {
//            _intents.onNext(ShareTransactionDetailIntent(transactionId))
//            true
//        }
//        else -> super.onOptionsItemSelected(item)
//    }

    private fun setupObserver() {
        compositeBag += vm.effects().subscribe(this::handleViewEffects)
        vm.processIntents(intents())
    }

    override fun intents(): Observable<TransactionDetailIntent> {
        return intents
    }

    private val intentLambda: (TransactionDetailIntent) -> Unit = { intent ->
        _intents.onNext(intent)
    }

    override fun render(state: TransactionDetailViewState) {
        if (state.showLoadingView) {
            transaction_detail_loader.show()
        } else {
            transaction_detail_loader.hide()
        }

        if (state.transaction.packetName == "PUBLISH") {
            searchMenuItemVisible = true
            requireActivity().invalidateOptionsMenu()
        }

        with(state.transaction) {
            packet_info.visibility =
                if (packetInfo.isEmpty()) View.GONE else View.VISIBLE
            packet_info.text = HtmlCompat.fromHtml(
                packetInfo,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            originalBody = packetBody
            packet_body.text = packetBody

            // enableCopyOption(packetInfo)
        }
    }

    private fun enableCopyOption(bodyString: String?) {
        if (bodyString.isNullOrBlank().not()) {
            copy.apply {
                visibility = View.VISIBLE
                setOnClickListener { copyText(bodyString) }
            }
        }
    }

    private fun copyText(text: String?) {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (null != clipboardManager) {
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.mqtt_chuck_name),
                    text
                )
            )
            Snackbar.make(copy, R.string.mqtt_chuck_body_content_copied, Snackbar.LENGTH_SHORT)
                .show()
        } else {
            Snackbar.make(copy, R.string.mqtt_chuck_body_content_copy_failed, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleViewEffects(effect: TransactionDetailViewEffect) {
        when (effect) {
            is TransactionDetailViewEffect.ShareTransactionDetailViewEffect -> {
                share(effect.mqttTransactionUiModel.shareText)
            }
        }
    }

    private fun share(transactionDetailsText: String) {
        startActivity(
            ShareCompat.IntentBuilder.from(requireActivity())
                .setType(MIME_TYPE)
                .setChooserTitle(getString(R.string.mqtt_chuck_share_transaction_title))
                .setSubject(getString(R.string.mqtt_chuck_share_transaction_subject))
                .setText(transactionDetailsText)
                .createChooserIntent()
        )
    }

    companion object {
        private const val MIME_TYPE = "text/plain"
        private const val EXTRA_TRANSACTION_ID = "transaction_id"

        @JvmStatic
        fun newInstance(transactionId: Long) =
            TransactionDetailFragment()
                .apply {
                    arguments = Bundle().apply {
                        putLong(EXTRA_TRANSACTION_ID, transactionId)
                    }
                }
    }

    override fun onQueryTextSubmit(query: String): Boolean = false

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.isNotBlank()) {
            packet_body.text = originalBody?.highlightWithDefinedColors(
                newText,
                backgroundSpanColor,
                foregroundSpanColor
            )
        } else {
            packet_body.text = originalBody
        }
        return true
    }
}
