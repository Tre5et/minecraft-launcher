package dev.treset.treelauncher.backend.util

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class EmptyingJobQueue<A>(
    private val onEmptied: () -> Unit,
    private val argumentSupplier: () -> A
) {
    private var finishProcessing = false
    private val queue: BlockingQueue<(A) -> Unit> = LinkedBlockingQueue()

    init {
        Thread {
            while(true) {
                if(queue.isEmpty()) {
                    onEmptied()
                    if(finishProcessing) {
                        return@Thread
                    }
                }
                queue.take()(argumentSupplier())
            }
        }.start()
    }

    fun add(element: (A) -> Unit) {
        queue.add(element)
    }

    fun finish() {
        finishProcessing = true
    }
}