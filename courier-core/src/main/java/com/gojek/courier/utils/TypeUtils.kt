@file:JvmName("TypeUtils")

package com.gojek.courier.utils

import androidx.annotation.RestrictTo
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Type.getRawType(): Class<*> = Utils.getRawType(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Type.hasUnresolvableType(): Boolean =
    Utils.hasUnresolvableType(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun ParameterizedType.getParameterUpperBound(index: Int): Type =
    Utils.getParameterUpperBound(index, this)
