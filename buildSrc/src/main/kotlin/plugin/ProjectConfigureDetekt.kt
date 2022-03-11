package plugin

import deps
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.detekt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import versions

internal fun Project.configureDetekt() {
    project.apply<DetektPlugin>()

    project.afterEvaluate {
        val configFile = "$rootDir/buildSrc/detekt/detekt.yml"

        detekt {
            toolVersion = versions.detekt
            input = files(
                "src/main/kotlin",
                "src/test/kotlin",
                "src/androidTest/kotlin"
            )
            config = files(configFile)
            reports {
                xml {
                    enabled = true
                    destination = file("${rootDir}/report/${project.name}/detekt/detekt.xml")
                }
                html {
                    enabled = true
                    destination = file("${rootDir}/report/${project.name}/detekt/detekt.html")
                }
            }
        }

        dependencies {
            add("detekt", deps.detekt.cli)
            add("detektPlugins", deps.detekt.lint)
        }
    }
}