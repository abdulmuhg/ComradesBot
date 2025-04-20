package utils

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * Manages coroutines throughout the application.
 * Provides structured concurrency and handles graceful shutdown.
 */
object CoroutineManager {
    private val logger = LoggerFactory.getLogger(CoroutineManager::class.java)

    // Main supervisor job that all coroutines are children of
    private val supervisorJob = SupervisorJob()

    // Different dispatchers for different types of work
    private val defaultDispatcher = Dispatchers.Default
    private val ioDispatcher = Dispatchers.IO

    // Main scope used for long-lived coroutines (tied to application lifecycle)
    private val applicationScope = CoroutineScope(supervisorJob + defaultDispatcher)

    // Custom exception handler to log unhandled exceptions in coroutines
    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        logger.error("Unhandled exception in coroutine [${context[CoroutineName]?.name ?: "unnamed"}]", throwable)
    }

    /**
     * Launches a new coroutine in the application scope that will be automatically cancelled
     * when the application shuts down.
     */
    fun launchInApplicationScope(
        name: String,
        context: CoroutineContext = defaultDispatcher,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return applicationScope.launch(context + CoroutineName(name) + exceptionHandler) {
            try {
                logger.debug("Starting coroutine: $name")
                block()
            } catch (e: CancellationException) {
                logger.debug("Coroutine $name was cancelled")
                throw e // Rethrow CancellationException to ensure proper coroutine cancellation
            } catch (e: Exception) {
                logger.error("Error in coroutine $name", e)
                throw e
            } finally {
                logger.debug("Coroutine completed: $name")
            }
        }
    }

    /**
     * Launches a coroutine for IO-bound operations.
     */
    fun launchIO(
        name: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return launchInApplicationScope(name, ioDispatcher, block)
    }

    /**
     * Shuts down all coroutines managed by this manager.
     * Should be called when the application is shutting down.
     */
    fun shutdown(timeoutMs: Long = 5000) {
        logger.info("Shutting down coroutines...")

        // Cancel the supervisor job to start graceful shutdown
        supervisorJob.cancel()

        // Wait for coroutines to complete, but with a timeout
        runBlocking {
            val shutdownJob = launch {
                try {
                    supervisorJob.join() // Wait for all coroutines to complete
                    logger.info("All coroutines shut down successfully")
                } catch (e: Exception) {
                    logger.error("Error during coroutine shutdown", e)
                }
            }

            // Set a timeout
            try {
                withTimeout(timeoutMs) {
                    shutdownJob.join()
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("Coroutine shutdown timed out after ${timeoutMs}ms")
                shutdownJob.cancel()
            }
        }
    }
}