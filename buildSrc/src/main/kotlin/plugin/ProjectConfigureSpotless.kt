package plugin

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

internal fun Project.configureSpotless() {
    project.apply<SpotlessPlugin>()

    this.extensions.getByType<SpotlessExtension>().run {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint("0.43.0")
                .setUseExperimental(true)
                .userData(
                    mapOf(
                        "android" to "true",
                        "max_line_length" to "off"
                    )
                )
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