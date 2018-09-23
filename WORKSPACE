workspace(name = "devsync")

GMAVEN_TAG = "20180513-1"

http_archive(
    name = "gmaven_rules",
    strip_prefix = "gmaven_rules-%s" % GMAVEN_TAG,
    url = "https://github.com/bazelbuild/gmaven_rules/archive/%s.tar.gz" % GMAVEN_TAG,
)

load("@gmaven_rules//:gmaven.bzl", "gmaven_rules")

gmaven_rules()

git_repository(
    name = "android_sdk_downloader",
    remote = "https://github.com/quittle/bazel_android_sdk_downloader",
    commit = "08413db0cbc8f27f39afc182dd44f5f53dbdbf9d",
)

load("@android_sdk_downloader//:rules.bzl", "android_sdk_repository")

android_sdk_repository(
    name = "androidsdk",
    workspace_name = "devsync",
    api_level = 28,
    build_tools_version = "28.0.2",
)

maven_jar(
    name = "com_google_guava_guava",
    artifact = "com.google.guava:guava:20.0"
)
