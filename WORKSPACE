workspace(name = "devsync")

git_repository(
    name = "android_sdk_downloader",
    remote = "https://github.com/quittle/bazel_android_sdk_downloader",
    commit = "08413db0cbc8f27f39afc182dd44f5f53dbdbf9d",
)

load("@android_sdk_downloader//:rules.bzl", "android_sdk_repository")

android_sdk_repository(
    name = "androidsdk",
    workspace_name = "devsync",
    api_level = 26,
    build_tools_version = "26.0.3",
)

maven_jar(
    name = "com_google_guava_guava",
    artifact = "com.google.guava:guava:20.0"
)