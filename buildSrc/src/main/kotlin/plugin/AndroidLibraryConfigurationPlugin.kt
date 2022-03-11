package plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConfigurationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureAndroid()
        project.configureAndroidExperimental()
        project.configureUnitTest()
        project.configureJacocoAndroid()
        project.configureDetekt()
    }
}