package com.quittle.rds

import android.util.Log
import java.io.BufferedReader

private const val TAG = "Logcat"

/**
 * Abstraction for watching the Android logcat. Its functionality may be limited in later versions
 * of Android.
 * @see This <a href="https://stackoverflow.com/q/22127349">StackOverflow question</a> that explains
 * when and how things were restricted.
 */
class Logcat() {
    private val packageToWatchers = HashMap<String, Set<LogcatWatcher>>()
    private val watcherToPackage = HashMap<LogcatWatcher, String>()
    private val thread : Thread = Thread(LogcatWatcherThread())
    private var backgroundThreadRunning : Boolean = false

    private inner class LogcatWatcherThread : Runnable {

        override fun run() {
            try {
                val process : Process = Runtime.getRuntime().exec("logcat")
                val reader : BufferedReader = process.getInputStream().bufferedReader()

                reader.lineSequence().forEach { line: String ->
                    for (watcher in watcherToPackage.keys) {
                        watcher.onMessage(line)
                    }
                }
                reader.close()
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to read logcat", e)
            }
        }
    }

    /**
     * Adds a watcher for monitoring the logcat
     * @param packageId Not current used but intended to be the Android app package id to subscribe
     *                  to.
     * @param watcher Receives callbacks on new logcat events
     */
    fun addWatcher(packageId: String, watcher: LogcatWatcher) {
        watcherToPackage.put(watcher, packageId)
        startIfNecessay()
    }

    /**
     * Unsubscribe the watcher from recieving updates
     * @param watcher The watcher to unsubscribe
     */
    fun removeWatcher(watcher: LogcatWatcher) {
        watcherToPackage.remove(watcher)
        stopIfNecessary()
    }

    /**
     * Unsubscribes all watchers and stops the background thread. After running, the instance may be
     * reused.
     */
    fun clearAndStop() {
        watcherToPackage.clear()
        packageToWatchers.clear()
        stopIfNecessary()
    }

    private fun stopIfNecessary() {
        if (backgroundThreadRunning && watcherToPackage.isEmpty()) {
            thread.interrupt()
            thread.join()
        }
    }

    private fun startIfNecessay() {
        if (!backgroundThreadRunning && !watcherToPackage.isEmpty()) {
            thread.start();
            backgroundThreadRunning = true
        }
    }
}
