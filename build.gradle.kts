buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:4.1.3")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.31")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:2.31.2-alpha")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.4")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven ("https://dl.bintray.com/qiscustech/maven" )
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}