package scripts

import scripts.Variants_gradle.BuildTypes

private object Default {
    const val BUILD_TYPE = BuildTypes.DEBUG

    val VARIANT = BUILD_TYPE.capitalize()
}

tasks.register("runUnitTests", Exec::class) {
    description = "Runs all Unit Tests."
    commandLine("$rootDir/scripts/runUnitTests.sh")
}

tasks.register("runRegressionTests", Exec::class) {
    description = "Runs all Unit Tests and Integration Test."
    commandLine("$rootDir/scripts/runRegressionTests.sh")
}

tasks.register("publishMavenLocal", Exec::class) {
    description = "Publish Libraries to Maven Local."
    commandLine("$rootDir/scripts/publishMavenLocal.sh")
}

tasks.register("publishDokka", Exec::class) {
    description = "Publish Libraries to Maven Local."
    commandLine("$rootDir/scripts/publishDokka.sh")
}
