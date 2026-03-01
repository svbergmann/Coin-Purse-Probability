plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(25)

    js {
        binaries.executable()
        compilerOptions {
            main.set(org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode.CALL)
        }
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
