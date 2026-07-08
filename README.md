# Release Tag Version Plugin

A Gradle plugin that uses the latest git semantic version tag to set the version code and name for an Android application.

## Usage

1.  Apply the plugin to your `build.gradle.kts` file:
    ```kotlin
    plugins {
      id("com.eygraber.release-tag-version") version "0.7.10"
    }
    ```

2.  The plugin will automatically use the latest git semantic version tag to set the version code and version name.

The plugin is compatible with the
[configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) and
[isolated projects](https://docs.gradle.org/current/userguide/isolated_projects.html).

## Git Tag Format

For the plugin to work, there needs to be at least one [semantic version](https://semver.org/) tag,
for example `1.2.3+45`. The plugin will parse the `versionName` from the `MAJOR.MINOR.PATCH`, and the
`versionCode` from numeric `metadata` of the latest version tag.

The latest tag is chosen by semantic version precedence. Because semver precedence ignores build
metadata, tags that share a `MAJOR.MINOR.PATCH` (e.g. successive builds `1.2.3+45` and `1.2.3+46`)
are ranked by their numeric build metadata, so the one with the highest `versionCode` wins.

## Non-release builds

For non-release build types, if the version is inferred from a git tag, the calculated version code is
incremented by one. This is done to ensure that a non-release build always has a higher version code than the
corresponding release build.

## Configuration

The plugin can be configured using the `releaseTagVersion` extension in your `build.gradle.kts` file.

### Overrides

There are several ways to override the version information inferred from the git tag. 
The following is the order of precedence for overrides, from highest to lowest:

1.  `versionOverride`: A Gradle property that overrides both the version name and version code.
2.  `versionCodeOverride` and `versionNameOverride`: Gradle properties that override the version code and 
    version name individually.
3.  `versionOverrideFile`: A file that contains a version string to override both the version name and
    (optionally) the version code.

#### `versionOverride`

A Gradle property that can be used to override both the version code and name.
The property should contain a version string in the format `<versionName>+<versionCode>`.

Example:
```bash
./gradlew assemble -PversionOverride=1.2.3+45
```

#### `versionCodeOverride` and `versionNameOverride`

Gradle properties that can be used to override the version code and version name individually. 
This is useful when you want to override only one part of the version information, while still inferring 
the other from the git tag.

Example:
```bash
./gradlew assemble -PversionCodeOverride=46 -PversionNameOverride=1.2.3
```

#### `versionOverrideFile`

A file that can be used to override the version name and (optionally) the version code.
The file should contain a version string in the format `<versionName>+<versionCode>`.

Example:

.version-override
```
1.2.3+45
```

### Other Options

```kotlin
releaseTagVersion {
  // Whether or not the version code should be inferred from git tags.
  // Default: true
  versionCodeIsInferred.set(true)

  // Whether or not the version name should be inferred from git tags.
  // Default: true
  versionNameIsInferred.set(true)

  // The version code to use if no git tag is found.
  // Default: 1
  fallbackVersionCode.set(1)

  // The version name to use if no git tag is found.
  // Default: "1.0.0"
  fallbackVersionName.set("1.0.0")

  // A prefix to use when searching for git tags.
  // Default: "" (empty string)
  versionPrefix.set("")

  // The build types that are considered "release" builds.
  // For these build types, the version code will be used as-is.
  // For other build types (e.g., "debug"), the version code will be incremented by 1
  // if it comes from a git tag.
  // Default: setOf("release")
  releaseBuildTypes.set(setOf("release"))
}
```
