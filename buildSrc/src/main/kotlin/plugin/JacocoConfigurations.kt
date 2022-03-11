package plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

fun Project.configureJacocoAndroid() {
    configureJacoco {
        tasks.register<JacocoReport>(this) {
            setDependsOn(setOf("testDebugUnitTest"))
            group = "verification"
            description = "Generate jacoco test report for android"

            val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug")
            val mainSrc = "${project.projectDir}/src/main/kotlin"
            var excludedFiles = emptyList<String>()
            if (project.extra.has("fileFilter")) {
                excludedFiles = project.extra.get("fileFilter") as List<String>
            }

            sourceDirectories.setFrom(files(mainSrc))
            classDirectories.setFrom(debugTree.apply {
                setExcludes(excludedFiles)
            })

            executionData.setFrom(fileTree(buildDir).apply {
                setIncludes(setOf("jacoco/testDebugUnitTest.exec"))
            })
        }
    }

    tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        setDependsOn(setOf("jacocoTestReport"))
        group = "verification"
        description = "Runs jacoco test verification for android"

        violationRules {
            rule {
                limit {
                    var minimumCoverage = "0.0"
                    if (project.extra.has("minimumCoverage")) {
                        minimumCoverage = project.extra.get("minimumCoverage").toString()
                    }
                    minimum = minimumCoverage.toBigDecimal()
                }
            }
        }

        val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug")
        val mainSrc = "${project.projectDir}/src/main/kotlin"
        var excludedFiles = emptyList<String>()
        if (project.extra.has("fileFilter")) {
            excludedFiles = project.extra.get("fileFilter") as List<String>
        }

        sourceDirectories.setFrom(files(mainSrc))
        classDirectories.setFrom(debugTree.apply {
            setExcludes(excludedFiles)
        })

        executionData.setFrom(fileTree(buildDir).apply {
            setIncludes(setOf("jacoco/testDebugUnitTest.exec"))
        })
    }
}

fun Project.configureJacocoJvm() {
    configureJacoco {
        tasks.named<JacocoReport>(this).configure {
            dependsOn(tasks.named("test"))
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification").configure {
        dependsOn(tasks.named("jacocoTestReport"))
        group = "verification"
        description = "Runs jacoco test verification for android"

        violationRules {
            rule {
                limit {
                    var minimumCoverage = "0.0"
                    if (project.extra.has("minimumCoverage")) {
                        minimumCoverage = project.extra.get("minimumCoverage").toString()
                    }
                    minimum = minimumCoverage.toBigDecimal()
                }
            }
        }

        val debugTreeJava = fileTree("${project.buildDir}/classes/java")
        val debugTreeKotlin = fileTree("${project.buildDir}/classes/kotlin")
        val mainSrc = "${project.projectDir}/src/main/kotlin"

        sourceDirectories.setFrom(files(mainSrc))
        classDirectories.setFrom(files(debugTreeJava, debugTreeKotlin))

        executionData.setFrom(fileTree(buildDir).apply {
            setIncludes(setOf("jacoco/test.exec"))
        })
    }
}

private fun Project.configureJacoco(configuration: String.() -> Unit) {
    apply<JacocoPlugin>()

    extensions.getByType(JacocoPluginExtension::class.java).toolVersion = versions.jacoco

    val task = "jacocoTestReport"

    task.configuration()

    tasks.named<JacocoReport>(task).configure {
        reports.apply {
            xml.apply {
                isEnabled = true
                destination = File("${project.buildDir}/reports/jacocoTestReport.xml")
            }
            html.apply {
                isEnabled = true
                destination = File("${project.buildDir}/reports/jacoco")
            }
        }
    }
}
