import com.eygraber.conventions.kotlin.KotlinFreeCompilerArg
import com.eygraber.conventions.kotlin.KotlinOptIn
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  `kotlin-dsl`
  alias(libs.plugins.conventionsKotlin)
  alias(libs.plugins.conventionsDetekt)
  alias(libs.plugins.conventionsPublishToMavenCentral)
  alias(libs.plugins.dependencyAnalysis)
}

kotlin {
  @OptIn(ExperimentalAbiValidation::class)
  abiValidation.enabled = true
}

gradlePlugin {
  plugins {
    create("releaseTagVersion") {
      id = "com.eygraber.release-tag-version"
      implementationClass = "com.eygraber.ReleaseTagVersionPlugin"
    }
  }
}

// See https://github.com/gradle/gradle/issues/22466
val additionalPluginClasspath: Configuration by configurations.creating

dependencies {
  compileOnly(libs.buildscript.android)

  implementation(libs.semver)

  testImplementation(gradleTestKit())
  testImplementation(libs.test.junit)
  testImplementation(libs.test.kotest.assertions.core)
  testImplementation(libs.test.parameterInjector)

  additionalPluginClasspath(libs.buildscript.android)
}

tasks {
  pluginUnderTestMetadata {
    pluginClasspath.from(additionalPluginClasspath)
  }

  test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
  }
}

gradleConventions {
  kotlin {
    freeCompilerArgs += KotlinFreeCompilerArg.Unknown("-Xmulti-dollar-interpolation")
    freeCompilerArgs += KotlinFreeCompilerArg.Unknown("-Xcontext-parameters")
  }
}
