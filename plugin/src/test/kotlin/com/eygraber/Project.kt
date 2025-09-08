package com.eygraber

import org.intellij.lang.annotations.Language
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

class Project(
  private val testProjectDir: TemporaryFolder,
) {
  val projectRoot = testProjectDir.root

  fun writeVersionOverrideFile(
    version: String,
    name: String = ".version-override",
  ) {
    testProjectDir.newFile(name).writeText(version)
  }

  fun writeBuildFiles(
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
      set("org.gradle.caching", "false")
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
      |
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

  fun appendToBuildFile(
    @Language("kotlin")
    appendedContent: String = "",
  ) {
    val buildFile = File(testProjectDir.root, "build.gradle.kts")
    buildFile.appendText(appendedContent)
  }
}
