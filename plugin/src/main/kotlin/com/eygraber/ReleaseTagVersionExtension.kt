package com.eygraber

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Configures the release tag version plugin.
 */
interface ReleaseTagVersionExtension {
  /**
   * Whether or not the version name should be inferred from git tags.
   *
   * Default: `true`
   */
  val versionNameIsInferred: Property<Boolean>

  /**
   * Whether or not the version code should be inferred from git tags.
   *
   * Default: `true`
   */
  val versionCodeIsInferred: Property<Boolean>

  /**
   * A file that can be used to override the version name and code.
   *
   * Default: `projectDir/.version-override`
   */
  val versionOverrideFile: RegularFileProperty

  /**
   * The version code to use if no git tag is found.
   *
   * Default: `1`
   */
  val fallbackVersionCode: Property<Int>

  /**
   * The version name to use if no git tag is found.
   *
   * Default: `"1.0.0"`
   */
  val fallbackVersionName: Property<String>

  /**
   * A prefix to use when searching for git tags.
   *
   * Default: `""` (empty string)
   */
  val versionPrefix: Property<String>

  /**
   * A property that can be used to override the version code and name.
   *
   * Default: none
   */
  val versionOverride: Property<String>

  /**
   * A property that can be used to override the version code.
   *
   * Default: none
   */
  val versionCodeOverride: Property<Int>

  /**
   * A property that can be used to override the version name.
   *
   * Default: none
   */
  val versionNameOverride: Property<String>

  /**
   * The build types that are considered "release" builds.
   * For these build types, the version code will be used as-is.
   * For other build types (e.g., "debug"), the version code will be incremented by 1.
   *
   * Default: `setOf("release")`
   */
  val releaseBuildTypes: SetProperty<String>
}
