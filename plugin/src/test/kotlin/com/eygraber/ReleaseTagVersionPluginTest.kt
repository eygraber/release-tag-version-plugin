package com.eygraber

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ReleaseTagVersionPluginTest {
  @get:Rule
  val testProjectDir = TemporaryFolder()

  val git by lazy {
    Git(testProjectDir.root)
  }

  val project by lazy {
    Project(testProjectDir)
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val testKitDir = TemporaryFolder()
  }

  enum class Strategy {
    VersionOverride,
    VersionOverrideCli,
    VersionCodeOverride,
    VersionCodeOverrideCli,
    VersionNameOverride,
    VersionNameOverrideCli,
    VersionFileOverride,
    LatestGitTagWithPrefix,
    LatestGitTag,
    Fallback,
    NoVersionCodeInference,
    NoVersionNameInference,
    ;

    private val v
      get() = when(this) {
        VersionOverride -> "1.0.3+4"
        VersionOverrideCli -> "1.0.3+4"
        VersionCodeOverride -> "5"
        VersionCodeOverrideCli -> "5"
        VersionNameOverride -> "1.0.4"
        VersionNameOverrideCli -> "1.0.4"
        VersionFileOverride -> "1.0.2+3"
        LatestGitTagWithPrefix -> "1.0.0+2"
        LatestGitTag -> "1.0.0+2"
        Fallback -> "1.0.0+1"
        NoVersionCodeInference -> ""
        NoVersionNameInference -> ""
      }

    fun environmentSetup(
      git: Git,
      project: Project,
    ) {
      when(this) {
        Fallback,
        NoVersionCodeInference,
        NoVersionNameInference,
        -> Unit

        LatestGitTag -> git.init(v)
        LatestGitTagWithPrefix -> {
          git.init("v$v")
          git.tag("2.0.0+100")
        }

        else -> {
          git.init(LatestGitTag.v)
          project.writeVersionOverrideFile(VersionFileOverride.v)
        }
      }
    }

    fun projectSetup(
      project: Project,
    ) {
      when(this) {
        VersionOverride -> project.writeBuildFiles(
          """
            |releaseTagVersion {
            |  versionOverride.set("${VersionOverride.v}")
            |}
            """.trimMargin(),
        )

        VersionCodeOverride -> project.writeBuildFiles(
          """
            |releaseTagVersion {
            |  versionOverride.set("${VersionOverride.v}")
            |  versionCodeOverride.set(${VersionCodeOverride.v})
            |}
            """.trimMargin(),
        )

        VersionNameOverride -> project.writeBuildFiles(
          """
            |releaseTagVersion {
            |  versionOverride.set("${VersionOverride.v}")
            |  versionNameOverride.set("${VersionNameOverride.v}")
            |}
            """.trimMargin(),
        )

        LatestGitTagWithPrefix -> project.writeBuildFiles(
          """
          |releaseTagVersion {
          |  versionPrefix.set("v")
          |}
          """.trimMargin(),
        )

        NoVersionCodeInference -> project.writeBuildFiles(
          """
          |android {
          |  defaultConfig {
          |    versionCode = 100
          |  }
          |}
          |
          |releaseTagVersion {
          |  versionCodeIsInferred.set(false)
          |}
          """.trimMargin(),
        )

        NoVersionNameInference -> project.writeBuildFiles(
          """
          |android {
          |  defaultConfig {
          |    versionName = "2.0.0"
          |  }
          |}
          |
          |releaseTagVersion {
          |  versionNameIsInferred.set(false)
          |}
          """.trimMargin(),
        )

        else -> project.writeBuildFiles()
      }
    }

    fun update(
      git: Git,
      project: Project,
      newStrategy: Strategy,
    ) {
      val updatedVersionCode = 6
      val updatedVersionName = "1.0.5"
      val updatedVersion = "1.0.5+6"

      if(newStrategy == LatestGitTag) {
        git.tag(updatedVersion)
      }

      when(newStrategy) {
        VersionOverride -> TODO()
        VersionOverrideCli -> TODO()
        VersionCodeOverride -> TODO()
        VersionCodeOverrideCli -> TODO()
        VersionNameOverride -> TODO()
        VersionNameOverrideCli -> TODO()
        VersionFileOverride -> TODO()
        LatestGitTagWithPrefix -> TODO()
        LatestGitTag -> TODO()
        Fallback -> TODO()
        NoVersionCodeInference -> TODO()
        NoVersionNameInference -> TODO()
      }
    }

    fun run(
      project: Project,
      task: String,
    ) = when(this) {
      VersionOverrideCli -> arrayOf("-PversionOverride=${VersionOverride.v}")

      VersionCodeOverrideCli -> arrayOf(
        "-PversionOverride=${VersionOverride.v}",
        "-PversionCodeOverride=${VersionCodeOverride.v}"
      )

      VersionNameOverrideCli -> arrayOf(
        "-PversionOverride=${VersionOverride.v}",
        "-PversionNameOverride=${VersionNameOverride.v}",
      )

      else -> emptyArray()
    }.let { args ->
      with(project) {
        runGradle(*(arrayOf(task) + args))
      }
    }

    fun assertResult(result: BuildResult) {
      val versionCode = when(this) {
        VersionCodeOverride,
        VersionCodeOverrideCli,
        -> "${VersionCodeOverride.v} from OverridingProperty"

        VersionOverride,
        VersionOverrideCli,
        VersionNameOverride,
        VersionNameOverrideCli,
        -> "${VersionOverride.v.split("+").last()} from OverridingProperty"

        VersionFileOverride -> "${v.split("+").last()} from OverridingFile"

        LatestGitTagWithPrefix,
        LatestGitTag,
        -> "${v.split("+").last()} from LatestGitTag"

        Fallback -> "${v.split("+").last()} from Fallback"

        NoVersionCodeInference -> ""
        NoVersionNameInference -> "${Fallback.v.split("+").last()} from Fallback"
      }

      val versionName = when(this) {
        VersionNameOverride,
        VersionNameOverrideCli,
        -> "${VersionNameOverride.v} from OverridingProperty"

        VersionOverride,
        VersionOverrideCli,
        VersionCodeOverride,
        VersionCodeOverrideCli,
        -> "${VersionOverride.v.split("+").first()} from OverridingProperty"

        VersionFileOverride -> "${v.split("+").first()} from OverridingFile"

        LatestGitTagWithPrefix,
        LatestGitTag,
        -> "${v.split("+").first()} from LatestGitTag"

        Fallback -> "${v.split("+").first()} from Fallback"

        NoVersionCodeInference -> "${Fallback.v.split("+").first()} from Fallback"
        NoVersionNameInference -> ""
      }

      result.output shouldNotContain "> Task :inferReleaseVersion UP-TO-DATE"

      when(NoVersionCodeInference) {
        this -> result.output shouldNotContain "Using versionCode"
        else -> result.output shouldContain "Using versionCode $versionCode"
      }

      when(NoVersionNameInference) {
        this -> result.output shouldNotContain "Using versionName"
        else -> result.output shouldContain "Using versionName $versionName"
      }
    }

    context(project: Project)
    private fun runGradle(vararg args: String) =
      GradleRunner
        .create()
        .withProjectDir(project.projectRoot)
        .withArguments(*arrayOf(*args))
        .withPluginClasspath()
        .withTestKitDir(testKitDir.root)
        .build()
  }

  @Test
  fun runningUsesTheCorrectVersionCodeAndNameFor(
    @TestParameter
    strategy: Strategy,
  ) {
    strategy.environmentSetup(git, project)
    strategy.projectSetup(project)
    val result = strategy.run(
      project = project,
      task = "assembleRelease",
    )

    strategy.assertResult(result)

    val newResult = strategy.run(
      project = project,
      task = "assembleRelease",
    )

    newResult.output shouldContain "> Task :inferReleaseVersion UP-TO-DATE"
    newResult.output shouldContain "Reusing configuration cache."
  }

  // @Test
  // fun runningWithANewStrategyUsesTheCorrectVersionCodeAndNameFor(
  //   @TestParameter
  //   strategy: Strategy,
  //   @TestParameter
  //   newStrategy: Strategy,
  //
  //   ) {
  //   strategy.environmentSetup(git, project)
  //   strategy.projectSetup(project)
  //   val result = strategy.run(
  //     project = project,
  //     task = "assembleRelease",
  //   )
  //
  //   strategy.assertResult(result)
  //
  //   val newResult = strategy.run(
  //     project = project,
  //     task = "assembleRelease",
  //   )
  //
  //   newResult.output shouldContain "> Task :inferReleaseVersion UP-TO-DATE"
  //   newResult.output shouldContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionOverrideFile without a version code uses the git tag`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.0.0+1")
  //   project.writeVersionOverrideFile("1.2.3")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from OverridingFile"
  //
  //   ensureConfigurationCacheReuse("assembleRelease")
  // }
  //
  // @Test
  // fun `versionOverrideFile without a numeric version code uses the fallback`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.0.0+1")
  //   project.writeVersionOverrideFile("1.2.3+abc")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from OverridingFile"
  //
  //   ensureConfigurationCacheReuse("assembleRelease")
  // }
  //
  // @Test
  // fun `git tag without version code is used`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from Fallback"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   ensureConfigurationCacheReuse("assembleRelease")
  // }
  //
  // @Test
  // fun `git tag without a numeric version code uses the fallback`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+abc")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from Fallback"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   ensureConfigurationCacheReuse("assembleRelease")
  // }
  //
  // @Test
  // fun `debug build type increments version code`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleDebug")
  //
  //   result.output shouldContain "Using versionCode 5 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   ensureConfigurationCacheReuse("assembleDebug")
  // }
  //
  // @Test
  // fun `custom releaseBuildTypes is used`() {
  //   project.writeBuildFiles(
  //     """
  //     |android {
  //     |  buildTypes {
  //     |    create("staging") {
  //     |      matchingFallbacks += "debug"
  //     |    }
  //     |  }
  //     |}
  //     |
  //     |releaseTagVersion {
  //     |  releaseBuildTypes.set(setOf("staging"))
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   git.init("1.2.3+4")
  //
  //   val assembleStagingResult = runGradle("assembleStaging")
  //
  //   assembleStagingResult.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   assembleStagingResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   ensureConfigurationCacheReuse("assembleStaging")
  // }
  //
  // @Test
  // fun `versionCodeIsInferred invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   project.appendToBuildFile(
  //     """
  //     |android {
  //     |  defaultConfig {
  //     |    versionCode = 5
  //     |  }
  //     |}
  //     |
  //     |releaseTagVersion {
  //     |  versionCodeIsInferred.set(false)
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldNotContain "Using versionCode"
  //   nextResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionNameIsInferred invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   project.appendToBuildFile(
  //     """
  //     |android {
  //     |  defaultConfig {
  //     |    versionName = "1.0.0"
  //     |  }
  //     |}
  //     |
  //     |releaseTagVersion {
  //     |  versionNameIsInferred.set(false)
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleRelease")
  //   nextResult.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   nextResult.output shouldNotContain "Using versionName"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionOverride invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   val nextResult = runGradle("assembleRelease", "-PversionOverride=1.2.4+5")
  //
  //   nextResult.output shouldContain "Using versionCode 5 from OverridingProperty"
  //   nextResult.output shouldContain "Using versionName 1.2.4 from OverridingProperty"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionCodeOverride invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   val nextResult = runGradle("assembleRelease", "-PversionCodeOverride=5")
  //
  //   nextResult.output shouldContain "Using versionCode 5 from OverridingProperty"
  //   nextResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionNameOverride invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   val nextResult = runGradle("assembleRelease", "-PversionNameOverride=1.2.4")
  //
  //   nextResult.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   nextResult.output shouldContain "Using versionName 1.2.4 from OverridingProperty"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `a versionOverrideFile invalidates the task and the configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   project.writeVersionOverrideFile("1.2.4+5")
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldContain "Using versionCode 5 from OverridingFile"
  //   nextResult.output shouldContain "Using versionName 1.2.4 from OverridingFile"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `a new git tag invalidates the task but not the configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   git.tag("1.2.4+5")
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldContain "Using versionCode 5 from LatestGitTag"
  //   nextResult.output shouldContain "Using versionName 1.2.4 from LatestGitTag"
  //   nextResult.output shouldContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `a new git commit does not invalidate the task or the configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   git.commit("a new commit")
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldNotContain "Using versionCode 4 from LatestGitTag"
  //   nextResult.output shouldNotContain "Using versionName 1.2.3 from LatestGitTag"
  //   nextResult.output shouldContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `fallbackVersionCode invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from Fallback"
  //   result.output shouldContain "Using versionName 1.0.0 from Fallback"
  //
  //   project.appendToBuildFile(
  //     """
  //     |releaseTagVersion {
  //     |  fallbackVersionCode.set(2)
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldContain "Using versionCode 2 from Fallback"
  //   nextResult.output shouldContain "Using versionName 1.0.0 from Fallback"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `fallbackVersionName invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from Fallback"
  //   result.output shouldContain "Using versionName 1.0.0 from Fallback"
  //
  //   project.appendToBuildFile(
  //     """
  //     |releaseTagVersion {
  //     |  fallbackVersionName.set("2.0.0")
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldContain "Using versionCode 1 from Fallback"
  //   nextResult.output shouldContain "Using versionName 2.0.0 from Fallback"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `versionPrefix invalidates the task and configuration cache`() {
  //   project.writeBuildFiles()
  //
  //   git.init("v1.2.3+4", "p1.2.4+5")
  //
  //   val result = runGradle("assembleRelease")
  //
  //   result.output shouldContain "Using versionCode 1 from Fallback"
  //   result.output shouldContain "Using versionName 1.0.0 from Fallback"
  //
  //   project.appendToBuildFile(
  //     """
  //     |releaseTagVersion {
  //     |  versionPrefix.set("v")
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleRelease")
  //
  //   nextResult.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   nextResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // @Test
  // fun `releaseBuildTypes invalidates the task and configuration cache`() {
  //   project.writeBuildFiles(
  //     """
  //     |android {
  //     |  buildTypes {
  //     |    create("staging")
  //     |  }
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   git.init("1.2.3+4")
  //
  //   val result = runGradle("assembleStaging")
  //
  //   result.output shouldContain "Using versionCode 5 from LatestGitTag"
  //   result.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //
  //   project.appendToBuildFile(
  //     """
  //     |releaseTagVersion {
  //     |  releaseBuildTypes.set(setOf("staging"))
  //     |}
  //     """.trimMargin(),
  //   )
  //
  //   val nextResult = runGradle("assembleStaging")
  //
  //   nextResult.output shouldContain "Using versionCode 4 from LatestGitTag"
  //   nextResult.output shouldContain "Using versionName 1.2.3 from LatestGitTag"
  //   nextResult.output shouldNotContain "Reusing configuration cache."
  // }
  //
  // private fun ensureConfigurationCacheReuse(task: String) {
  //   val result = runGradle(task)
  //
  //   result.output shouldContain "Reusing configuration cache."
  // }
  //
  // private fun runGradle(vararg args: String) =
  //   GradleRunner
  //     .create()
  //     .withProjectDir(testProjectDir.root)
  //     .withArguments(*arrayOf(*args, "--info"))
  //     .withPluginClasspath()
  //     .withArguments()
  //     .withTestKitDir(testKitDir.root)
  //     .build()
}
