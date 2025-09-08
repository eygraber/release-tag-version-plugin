package com.eygraber

import java.io.File

class Git(
  private val dir: File,
) {
  fun init(
    vararg tags: String,
  ) {
    ProcessBuilder("git", "init")
      .directory(dir)
      .start()
      .apply {
        if(waitFor() != 0) {
          error("Failed to initialize git - ${errorReader().readText()}")
        }
      }

    commit("initial commit")

    tag(*tags)
  }

  fun commit(message: String) {
    ProcessBuilder(
      "git",
      "-c",
      "user.name=Test",
      "-c",
      "user.email=test@test.com",
      "commit",
      "--allow-empty",
      "-m",
      message,
    )
      .directory(dir)
      .start()
      .apply {
        if(waitFor() != 0) {
          error("Failed to create git commit - ${errorReader().readText()}")
        }
      }
  }

  fun tag(
    vararg tags: String,
  ) {
    tags.forEach { tag ->
      ProcessBuilder("git", "tag", tag)
        .directory(dir)
        .start()
        .apply {
          if(waitFor() != 0) {
            error("Failed to create git tag $tag - ${errorReader().readText()}")
          }
        }
    }
  }
}
