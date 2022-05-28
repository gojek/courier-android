package plugin

import com.diffplug.gradle.spotless.SpotlessPlugin
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

internal fun Project.configureSpotless() {
    project.apply<SpotlessPlugin>()

    this.extensions.getByType<SpotlessExtension>().run {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint("0.43.0").userData(mapOf("android" to "true"))
        }

        format("kts") {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")
        }

        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
        }
    }
}