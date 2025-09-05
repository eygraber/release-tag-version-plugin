package com.eygraber

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.util.Locale

/**
 * A Gradle plugin that uses the latest git tag to set the version code and name for an Android application.
 */
class ReleaseTagVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create<ReleaseTagVersionExtension>(
      "releaseTagVersion",
    ).applyConventions(project)

    project.configureVariants(extension)
  }

  private fun ReleaseTagVersionExtension.applyConventions(
    project: Project,
  ) = apply {
    versionCodeIsInferred.convention(true)
    versionNameIsInferred.convention(true)
    versionOverrideFile.convention(project.layout.projectDirectory.file(".version-override"))
    fallbackVersionCode.convention(1)
    fallbackVersionName.convention("1.0.0")
    versionOverride.convention(project.findProperty("versionOverride") as? String)
    versionCodeOverride.convention((project.findProperty("versionCodeOverride") as? String)?.toIntOrNull())
    versionNameOverride.convention(project.findProperty("versionNameOverride") as? String)
    versionPrefix.convention("")
    releaseBuildTypes.convention(setOf("release"))
  }

  private fun Project.configureVariants(extension: ReleaseTagVersionExtension) {
    plugins.withType<AppPlugin> {
      val appExtension = extensions.findByType<ApplicationExtension>()
      val defaultConfig = appExtension?.defaultConfig

      extensions.findByType<ApplicationAndroidComponentsExtension>()?.onVariants { variant ->
        processVariant(
          variant = variant,
          extension = extension,
          defaultConfig = defaultConfig,
        )
      }
    }
  }

  private fun Project.processVariant(
    variant: ApplicationVariant,
    extension: ReleaseTagVersionExtension,
    defaultConfig: ApplicationDefaultConfig?,
  ) {
    val versionCodeFile = objects.fileProperty().convention(
      layout.buildDirectory.file("generated/com/eygraber/release/tag/version/${variant.name}/code.txt"),
    )

    val versionNameFile = objects.fileProperty().convention(
      layout.buildDirectory.file("generated/com/eygraber/release/tag/version/${variant.name}/name.txt"),
    )

    val inferVersionTask = registerInferVersionTask(
      variant = variant,
      extension = extension,
      versionCodeFile = versionCodeFile,
      versionNameFile = versionNameFile,
    )

    var defaultVersionCode = defaultConfig?.versionCode
    var defaultVersionName = defaultConfig?.versionName

    afterEvaluate {
      defaultVersionCode = defaultConfig?.versionCode
      defaultVersionName = defaultConfig?.versionName
    }

    variant
      .outputs
      .forEach { output ->
        output.versionCode.set(
          extension.versionCodeIsInferred.flatMap { isInferred ->
            when {
              isInferred ->
                inferVersionTask
                  .flatMap(ReleaseTagVersionTask::versionCodeFile)
                  .map { it.asFile.readText().toInt() }

              else -> provider { defaultVersionCode }
            }
          },
        )

        output.versionName.set(
          extension.versionNameIsInferred.flatMap { isInferred ->
            when {
              isInferred ->
                inferVersionTask
                  .flatMap(ReleaseTagVersionTask::versionNameFile)
                  .map { it.asFile.readText() }

              else -> provider { defaultVersionName }
            }
          },
        )
      }
  }

  private fun Project.registerInferVersionTask(
    variant: Variant,
    extension: ReleaseTagVersionExtension,
    versionCodeFile: RegularFileProperty,
    versionNameFile: RegularFileProperty,
  ): TaskProvider<ReleaseTagVersionTask> {
    val taskName = "infer${variant.name.replaceFirstChar { it.uppercase(Locale.ROOT) }}Version"

    return tasks.register<ReleaseTagVersionTask>(taskName) {
      group = "Versioning"
      description = "Infers the version code and name from the latest git release tag and writes it to a file"

      val gitDir = rootProject.file(".git/refs/tags")
      if(gitDir.exists()) {
        this.gitDir.set(gitDir)
      }

      val tagsDir = rootProject.file(".git/refs/tags")
      if(tagsDir.exists()) {
        this.gitTagsDir.set(tagsDir)
      }

      val versionOverrideFile = project.file(".version-override")
      if(versionOverrideFile.exists()) {
        this.versionOverrideFile.set(versionOverrideFile)
      }

      this.versionCodeIsInferred.set(extension.versionCodeIsInferred)
      this.versionNameIsInferred.set(extension.versionNameIsInferred)
      this.buildType.set(variant.buildType)
      this.versionOverride.set(extension.versionOverride)
      this.versionCodeOverride.set(extension.versionCodeOverride)
      this.versionNameOverride.set(extension.versionNameOverride)
      this.releaseBuildTypes.set(extension.releaseBuildTypes)
      this.versionPrefix.set(extension.versionPrefix)
      this.fallbackVersionCode.set(extension.fallbackVersionCode)
      this.fallbackVersionName.set(extension.fallbackVersionName)
      this.versionCodeFile.set(versionCodeFile)
      this.versionNameFile.set(versionNameFile)
    }
  }
}
