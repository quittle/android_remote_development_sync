package com.quittle.rds

/**
 * Subscribes to updates for LogCat logs.
 */
interface LogcatWatcher {
    /**
     * Called when new log messages occur
     * @param messages A list of messages.
     */
    fun onMessages(messages: List<String>)
}

