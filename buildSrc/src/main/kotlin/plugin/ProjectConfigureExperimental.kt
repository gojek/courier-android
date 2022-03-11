package plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension

internal fun Project.configureAndroidExperimental() {
    this.extensions.getByType<AndroidExtensionsExtension>().run {
        isExperimental = true
    }
}