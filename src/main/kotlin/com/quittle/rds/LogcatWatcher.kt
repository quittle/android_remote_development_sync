package com.quittle.rds

/**
 * Subscribes to updates for LogCat logs.
 */
interface LogcatWatcher {
    /**
     * Called when a new log message occurs.
     * @param message The message logged to logcat.
     */
    fun onMessage(message: String)
}

