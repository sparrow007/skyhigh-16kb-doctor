package com.sparrow.plugin.worker

import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

/**
 * Parameters for the SO scan work action.
 */
interface SoScanParameters : WorkParameters {
    // For future parallel processing if needed
    // Currently, scanning is done sequentially in the main task
}

/**
 * Worker action for scanning native libraries in parallel.
 * This is a placeholder for potential future parallel processing optimizations.
 */
abstract class SoScanWorkAction : WorkAction<SoScanParameters> {

    override fun execute() {
        // Placeholder for parallel scanning logic
        // Current implementation handles scanning in the main task
        // This can be enhanced to process individual .so files in parallel
    }
}