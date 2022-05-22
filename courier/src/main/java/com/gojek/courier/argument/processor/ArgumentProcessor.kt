package com.gojek.courier.argument.processor

internal abstract class ArgumentProcessor() {
    abstract fun inject(args: Array<Any>)
}
