package com.gojek.courier.utils

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.lang.invoke.MethodHandles.Lookup
import java.lang.reflect.Method
import java.util.Optional

internal sealed class RuntimePlatform {

    open fun isDefaultMethod(method: Method): Boolean = false

    open fun invokeDefaultMethod(
        method: Method,
        declaringClass: Class<*>,
        proxy: Any,
        vararg args: Array<out Any>?
    ): Any =
        throw UnsupportedOperationException()

    class Default : RuntimePlatform()

    class Java8 : RuntimePlatform() {
        override fun isDefaultMethod(method: Method): Boolean = method.isDefault

        override fun invokeDefaultMethod(
            method: Method,
            declaringClass: Class<*>,
            proxy: Any,
            vararg args: Array<out Any>?
        ): Any {
            // Because the service interface might not be public, we need to use a MethodHandle lookup
            // that ignores the visibility of the declaringClass.
            val constructor = Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
            constructor.isAccessible = true
            return constructor.newInstance(declaringClass, -1 /* trusted */)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args)
        }
    }

    companion object {
        private val PLATFORM = findPlatform()

        fun get(): RuntimePlatform = PLATFORM

        private fun findPlatform(): RuntimePlatform = try {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Class.forName(Optional::class.java.name)
                Java8()
            } else {
                Default()
            }
        } catch (ignored: ClassNotFoundException) {
            Default()
        }
    }
}
