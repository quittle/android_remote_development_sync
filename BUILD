load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

android_binary(
    name = "test_binary",
    manifest = "src/main/AndroidManifest.xml",
    custom_package = "com.quittle.rds",
    srcs = glob(["src/main/java/**/*.java"]),
    resource_files = glob(["src/main/res/**/*"]),
    deps = [
        "@com_google_guava_guava//jar",
        gmaven_artifact("com.android.support:support-compat:aar:27.1.1"),
        gmaven_artifact("com.android.support:support-core-utils:aar:27.1.1"),
        gmaven_artifact("com.android.support:support-v4:aar:27.1.1"),
    ],
)
