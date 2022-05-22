package plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

internal fun Project.configureKtlint() {
    project.apply<KtlintPlugin>()

    extensions.getByType(KtlintExtension::class.java).version.set("0.45.2")
}