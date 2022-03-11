package plugin

import com.android.build.gradle.BaseExtension
import deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

internal fun Project.configureUnitTest() = this.extensions.getByType<BaseExtension>().run {
    dependencies {
        add("testImplementation", deps.android.test.junit)
        add("testImplementation", deps.android.test.mockito)
    }
}