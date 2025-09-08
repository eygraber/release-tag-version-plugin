package com.eygraber

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction

private data class VersionInfo(
  val code: Int?,
  val name: String?,
  val codeSource: VersionSource,
  val nameSource: VersionSource,
) {
  fun codeAndSource() = code?.let { it to codeSource }
  fun nameAndSource() = name?.let { it to nameSource }
}

private enum class VersionSource {
  Fallback,
  LatestGitTag,
  NotInferred,
  OverridingFile,
  OverridingProperty,
}

@CacheableTask
abstract class ReleaseTagVersionTask : DefaultTask() {
  @get:Input
  abstract val versionCodeIsInferred: Property<Boolean>

  @get:Input
  abstract val versionNameIsInferred: Property<Boolean>

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val versionOverrideFile: RegularFileProperty

  @get:Internal
  abstract val gitDir: DirectoryProperty

  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val gitTagsDir: DirectoryProperty

  @get:Input
  abstract val buildType: Property<String>

  @get:Input
  @get:Optional
  abstract val versionOverride: Property<String>

  @get:Input
  @get:Optional
  abstract val versionCodeOverride: Property<Int>

  @get:Input
  @get:Optional
  abstract val versionNameOverride: Property<String>

  @get:Input
  abstract val releaseBuildTypes: SetProperty<String>

  @get:Input
  abstract val versionPrefix: Property<String>

  @get:Input
  abstract val fallbackVersionCode: Property<Int>

  @get:Input
  abstract val fallbackVersionName: Property<String>

  @get:OutputFile
  abstract val versionCodeFile: RegularFileProperty

  @get:OutputFile
  abstract val versionNameFile: RegularFileProperty

  @TaskAction
  fun getVersion() {
    val isVersionCodeInferred = versionCodeIsInferred.get()
    val isVersionNameInferred = versionNameIsInferred.get()

    val overridingInfo =
      versionOverride
        .orNull
        ?.removePrefix(versionPrefix.get())
        ?.toVersionOrNull()
        ?.let { version ->
          extractVersionInfoFromSemVer(
            version = version,
            source = VersionSource.OverridingProperty,
          )
        }

    val overridingPropertiesInfo = VersionInfo(
      code = versionCodeOverride.orNull ?: overridingInfo?.code,
      name = versionNameOverride.orNull ?: overridingInfo?.name,
      codeSource = VersionSource.OverridingProperty,
      nameSource = VersionSource.OverridingProperty,
    )

    val overridingFileInfo =
      versionOverrideFile
        .orNull
        ?.asFile
        ?.takeIf { it.exists() }
        ?.readText()
        ?.removePrefix(versionPrefix.get())
        ?.toVersionOrNull()
        ?.let { version ->
          extractVersionInfoFromSemVer(
            version = version,
            source = VersionSource.OverridingFile,
          )
        }

    val latestGitReleaseInfo =
      getLatestGitReleaseTag()?.let { latestGitReleaseTag ->
        extractVersionInfoFromSemVer(
          version = latestGitReleaseTag,
          source = VersionSource.LatestGitTag,
        )
      }

    val fallbackInfo = VersionInfo(
      code = fallbackVersionCode.get(),
      name = fallbackVersionName.get(),
      codeSource = VersionSource.Fallback,
      nameSource = VersionSource.Fallback,
    )

    if(isVersionCodeInferred) {
      val (versionCode, source) =
        overridingPropertiesInfo.codeAndSource()
          ?: overridingFileInfo?.codeAndSource()
          ?: latestGitReleaseInfo?.codeAndSource()
          ?: fallbackInfo.codeAndSource()
          ?: error("Couldn't find a version code")

      val releaseBuildTypes = releaseBuildTypes.get()
      val buildType = buildType.get()

      val finalVersionCode = when(buildType) {
        in releaseBuildTypes -> versionCode

        else -> when(source) {
          VersionSource.Fallback,
          VersionSource.NotInferred,
          VersionSource.OverridingFile,
          VersionSource.OverridingProperty,
          -> versionCode

          VersionSource.LatestGitTag -> versionCode + 1
        }
      }

      versionCodeFile.get().asFile.apply {
        parentFile.mkdirs()
        writeText(
          finalVersionCode.toString().also {
            logger.lifecycle("Using versionCode $it from $source")
          },
        )
      }
    }
    else {
      versionCodeFile.get().asFile.delete()
    }

    if(isVersionNameInferred) {
      val (versionName, source) =
        overridingPropertiesInfo.nameAndSource()
          ?: overridingFileInfo?.nameAndSource()
          ?: latestGitReleaseInfo?.nameAndSource()
          ?: fallbackInfo.nameAndSource()
          ?: error("Couldn't find a version name")

      versionNameFile.get().asFile.apply {
        parentFile.mkdirs()
        writeText(
          versionName.also {
            logger.lifecycle("Using versionName $it from $source")
          },
        )
      }
    }
    else {
      versionNameFile.get().asFile.delete()
    }
  }

  private fun extractVersionInfoFromSemVer(
    version: Version,
    source: VersionSource,
  ) = VersionInfo(
    code = version.buildMetadata?.toIntOrNull(),
    name = version.toString().removeSuffix("+${version.buildMetadata}"),
    codeSource = source,
    nameSource = source,
  )

  private fun getLatestGitReleaseTag(): Version? {
    if(!gitDir.isPresent || !gitDir.get().asFile.exists()) return null
    if(!gitTagsDir.isPresent || !gitTagsDir.get().asFile.exists()) return null

    val process =
      ProcessBuilder("sh", "-c", "git tag -l")
        .directory(gitDir.get().asFile.parentFile)
        .start()

    val exitCode = process.waitFor()

    return when(exitCode) {
      0 ->
        process
          .inputStream
          .bufferedReader()
          .lineSequence()
          .filter { it.startsWith(versionPrefix.get()) }
          .mapNotNull { it.removePrefix(versionPrefix.get()).toVersionOrNull() }
          .maxOrNull()

      else -> null
    }
  }
}
