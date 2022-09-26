package com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ShareCompat
import androidx.fragment.app.createViewModelLazy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.presentation.base.fragment.FoodMviBaseFragment
import com.gojek.chuckmqtt.internal.presentation.servicelocator.MqttChuckPresentationDependencyLocator
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.ui.screen.TransactionDetailScreen
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel.TransactionDetailViewModel
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewState
import com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.screen.TransactionListScreen
import com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel.TransactionListViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlin.reflect.KClass

internal class TransactionListComposeFragment :
    FoodMviBaseFragment<TransactionListIntent, TransactionListViewState, TransactionListViewModel>() {

    private val transactionDetailViewModel by createViewModelLazy(
        viewModelClass = TransactionDetailViewModel::class,
        storeProducer = { viewModelStore },
        factoryProducer = { MqttChuckPresentationDependencyLocator.viewModelFactory }
    )

    override val clazz: KClass<TransactionListViewModel>
        get() = TransactionListViewModel::class

    private val applicationName: CharSequence
        get() = requireActivity().applicationInfo.loadLabel(requireActivity().packageManager)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "transactionListScreen"
                ) {
                    composable("transactionListScreen") {
                        val transactionListViewModel = vm

                        TransactionListScreen(
                            navController = navController,
                            toolbarSubtitle = applicationName,
                            viewModel = transactionListViewModel
                        )
                    }
                    composable("transactionDetailScreen/{transactionId}") { backStackEntry ->
                        backStackEntry.arguments?.getString("transactionId")?.let { transactionId ->
                            TransactionDetailScreen(
                                transactionId.toLong(),
                                transactionDetailViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
    }

    override fun onDestroyView() {
        compositeBag.clear()
        super.onDestroyView()
    }

    private fun setupObserver() {
        compositeBag += transactionDetailViewModel.effects()
            .subscribe(this::handleTransactionDetailViewEffects)
    }

    override fun intents(): Observable<TransactionListIntent> {
        return intents
    }

    override fun render(state: TransactionListViewState) {
    }

    private fun handleTransactionDetailViewEffects(effect: TransactionDetailViewEffect) {
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

        @JvmStatic
        fun newInstance() = TransactionListComposeFragment()
    }
}
