package com.gojek.chuckmqtt.internal.presentation.base.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.ViewModel
import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviIntent
import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.ViewState
import com.gojek.chuckmqtt.internal.presentation.servicelocator.MqttChuckPresentationDependencyLocator.viewModelFactory
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlin.reflect.KClass

internal abstract class FoodMviBaseFragment<I : MviIntent, S : ViewState, VM : ViewModel> : Fragment() {

    abstract val clazz: KClass<VM>

    val _intents: PublishSubject<I> = PublishSubject.create()
    val intents: Observable<I> = _intents.hide()

    val compositeBag = CompositeDisposable()

    val vm by createViewModelLazy(
        viewModelClass = clazz,
        storeProducer = { this.viewModelStore },
        factoryProducer = { viewModelFactory }
    )

    override fun onDestroy() {
        _intents.onComplete()
        super.onDestroy()
    }

    abstract fun intents(): Observable<I>

    abstract fun render(state: S)
}
