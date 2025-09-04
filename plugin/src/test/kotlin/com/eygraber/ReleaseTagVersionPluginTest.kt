package com.eygraber

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Properties

class ReleaseTagVersionPluginTest {
  @get:Rule
  val testProjectDir = TemporaryFolder()

  companion object {
    @get:ClassRule
    @JvmStatic
    val testKitDir = TemporaryFolder()
  }

  @Test
  fun `plugin applies successfully`() {
    writeBuildFiles()
    runGradle("tasks")
    ensureConfigurationCacheReuse("tasks")
  }

  @Test
  fun `versionCodeIsInferred false doesn't set the version code`() {
    writeBuildFiles(
      """
      |android {
      |  defaultConfig {
      |    versionCode = 5
      |  }
      |}
      |
      |releaseTagVersion {
      |  versionCodeIsInferred.set(false)
      |}
      """.trimMargin(),
    )

    setupGitTags("1.2.3")

    val result = runGradle("assembleRelease")

    result.output shouldNotContain "Using versionCode"
    result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionNameIsInferred false doesn't set the version name`() {
    writeBuildFiles(
      """
      |android {
      |  defaultConfig {
      |    versionName = "1.0.0"
      |  }
      |}
      |
      |releaseTagVersion {
      |  versionNameIsInferred.set(false)
      |}
      """.trimMargin(),
    )

    setupGitTags("1.2.3+5")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from LatestGitTag"
    result.output shouldNotContain "Using versionName"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionOverride is used`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionOverride.set("1.2.3+5")
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from OverridingProperty"
    result.output shouldContain "Using versionName 1.2.3 from OverridingProperty"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionCodeOverride is used`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionCodeOverride.set(5)
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from OverridingProperty"
    result.output shouldContain "Using versionName 1.0.0 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionNameOverride is used`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionNameOverride.set("5.4.3")
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+2")
    writeVersionOverrideFile("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from OverridingFile"
    result.output shouldContain "Using versionName 5.4.3 from OverridingProperty"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionCodeOverride and versionNameOverride are used`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionCodeOverride.set(5)
      |  versionNameOverride.set("5.4.3")
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from OverridingProperty"
    result.output shouldContain "Using versionName 5.4.3 from OverridingProperty"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionCodeOverride overrides versionOverride`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionOverride.set("1.2.3+5")
      |  versionCodeOverride.set(6)
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 6 from OverridingProperty"
    result.output shouldContain "Using versionName 1.2.3 from OverridingProperty"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionNameOverride overrides versionOverride`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionOverride.set("1.2.3+5")
      |  versionNameOverride.set("1.2.4")
      |}
      """.trimMargin(),
    )

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.0.0+1")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from OverridingProperty"
    result.output shouldContain "Using versionName 1.2.4 from OverridingProperty"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionOverrideFile is used`() {
    writeBuildFiles()

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.2.3+5")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 5 from OverridingFile"
    result.output shouldContain "Using versionName 1.2.3 from OverridingFile"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionOverrideFile without a version code uses the git tag`() {
    writeBuildFiles()

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.2.3")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from LatestGitTag"
    result.output shouldContain "Using versionName 1.2.3 from OverridingFile"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionOverrideFile without a numeric version code uses the fallback`() {
    writeBuildFiles()

    setupGitTags("1.0.0+1")
    writeVersionOverrideFile("1.2.3+abc")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from LatestGitTag"
    result.output shouldContain "Using versionName 1.2.3 from OverridingFile"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `git tag is used`() {
    writeBuildFiles()

    setupGitTags("1.2.3+4")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 4 from LatestGitTag"
    result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `git tag without version code is used`() {
    writeBuildFiles()

    setupGitTags("1.2.3")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from Fallback"
    result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `git tag without a numeric version code uses the fallback`() {
    writeBuildFiles()

    setupGitTags("1.2.3+abc")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from Fallback"
    result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `fallback version is used if no others are set`() {
    writeBuildFiles()
    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 1 from Fallback"
    result.output shouldContain "Using versionName 1.0.0 from Fallback"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `versionPrefix is used`() {
    writeBuildFiles(
      """
      |releaseTagVersion {
      |  versionPrefix.set("v")
      |}
      """.trimMargin(),
    )

    setupGitTags("1.2.2+4", "a1.2.3+5", "v1.2.4+6")

    val result = runGradle("assembleRelease")

    result.output shouldContain "Using versionCode 6 from LatestGitTag"
    result.output shouldContain "Using versionName 1.2.4 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleRelease")
  }

  @Test
  fun `debug build type increments version code`() {
    writeBuildFiles()

    setupGitTags("1.2.3+4")

    val result = runGradle("assembleDebug")

    result.output shouldContain "Using versionCode 5 from LatestGitTag"
    result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleDebug")
  }

  @Test
  fun `custom releaseBuildTypes is used`() {
    writeBuildFiles(
      """
      |android {
      |  buildTypes {
      |    create("staging") {
      |      matchingFallbacks += "debug"
      |    }
      |  }
      |}
      |
      |releaseTagVersion {
      |  releaseBuildTypes.set(setOf("staging"))
      |}
      """.trimMargin(),
    )

    setupGitTags("1.2.3+4")

    val assembleStagingResult = runGradle("assembleStaging")

    assembleStagingResult.output shouldContain "Using versionCode 4 from LatestGitTag"
    assembleStagingResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleStaging")

    val assembleDebugResult = runGradle("assembleDebug")

    assembleDebugResult.output shouldContain "Using versionCode 5 from LatestGitTag"
    assembleDebugResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"

    ensureConfigurationCacheReuse("assembleDebug")
  }

  private fun ensureConfigurationCacheReuse(task: String) {
    val result = runGradle(task)

    result.output shouldContain "Reusing configuration cache."
  }

  private fun runGradle(vararg args: String) =
    GradleRunner
      .create()
      .withProjectDir(testProjectDir.root)
      .withArguments(*arrayOf(*args, "--info"))
      .withPluginClasspath()
      .withTestKitDir(testKitDir.root)
      .build()

  private fun setupGitTags(
    vararg tags: String,
  ) {
    ProcessBuilder("git", "init")
      .directory(testProjectDir.root)
      .start()
      .waitFor()

    ProcessBuilder("git", "commit", "--allow-empty", "-m", "initial commit")
      .directory(testProjectDir.root)
      .start()
      .waitFor()

    tags.forEach { tag ->
      ProcessBuilder("git", "tag", tag)
        .directory(testProjectDir.root)
        .start()
        .waitFor()
    }
  }

  private fun writeVersionOverrideFile(version: String) {
    testProjectDir.newFile(".version-override").writeText(version)
  }

  private fun writeBuildFiles(
    @Language("kotlin")
    buildGradleContent: String = "",
  ) {
    val androidSdkHome = System.getenv("ANDROID_HOME")

    Properties().apply {
      set("sdk.dir", androidSdkHome)
      testProjectDir.newFile("local.properties").outputStream().use {
        store(it, null)
      }
    }

    Properties().apply {
      set("org.gradle.caching", "true")
      set("org.gradle.parallel", "true")
      set("org.gradle.configuration-cache", "true")
      set("org.gradle.configuration-cache.parallel", "true")
      testProjectDir.newFile("gradle.properties").outputStream().use {
        store(it, null)
      }
    }

    testProjectDir.newFile("settings.gradle.kts").writeText(
      """
      |pluginManagement {
      |  repositories {
      |    mavenLocal()
      |    google()
      |    mavenCentral()
      |    gradlePluginPortal()
      |  }
      |}
      |
      |dependencyResolutionManagement {
      |  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      |  repositories {
      |    google()
      |    mavenCentral()
      |  }
      |}
      |
      |rootProject.name = "test-project"
      """.trimMargin(),
    )

    testProjectDir.newFile("build.gradle.kts").writeText(
      $$"""
      |plugins {
      |  id("com.android.application")
      |  id("com.eygraber.release-tag-version")
      |}
      |
      |android {
      |  namespace = "com.test"
      |  compileSdk = 36
      |
      |  defaultConfig {
      |    minSdk = 21
      |  }
      |
      |  buildTypes {
      |    release {
      |      isMinifyEnabled = false
      |      isShrinkResources = false
      |    }
      |  }
      |
      |  lint {
      |    checkReleaseBuilds = false
      |  }
      |}
      |
      |$$buildGradleContent
      """.trimMargin(),
    )

    testProjectDir.newFolder("src/main")
    testProjectDir.newFile("src/main/AndroidManifest.xml").writeText(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
      """.trimMargin(),
    )
  }
}
