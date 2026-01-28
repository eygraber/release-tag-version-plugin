import com.eygraber.conventions.tasks.deleteRootBuildDirWhenCleaning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
  dependencies {
    classpath(libs.buildscript.android)
    classpath(libs.buildscript.detekt)
    classpath(libs.buildscript.dokka)
    classpath(libs.buildscript.kotlin)
    classpath(libs.buildscript.publish)
  }
}

plugins {
  base
  alias(libs.plugins.conventionsBase)
  alias(libs.plugins.dependencyAnalysisRoot)
}

deleteRootBuildDirWhenCleaning()

gradleConventionsDefaults {
  kotlin {
    jvmTargetVersion = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
    allWarningsAsErrors = true
  }
}

dependencyAnalysis {
  useTypesafeProjectAccessors(true)
}
