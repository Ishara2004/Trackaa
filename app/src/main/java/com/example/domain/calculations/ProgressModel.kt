package com.example.domain.calculations

data class DetailedProgress(
    val timeProgressPercent: Double?, // null if target is 0
    val actualFocusMinutes: Long,
    val targetFocusMinutes: Long,
    val taskProgressPercent: Double?, // null if 0 tasks
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val topicProgressPercent: Double?, // null if 0 topics
    val completedTopicsCount: Int,
    val totalTopicsCount: Int,
    val overallWeightedPercent: Double,
    val formulaExplanation: String
)

object ProgressModel {

    /**
     * Calculates three-dimensional progress and user-weighted overall progress.
     * Re-normalizes weights transparently if any metric is absent.
     */
    fun calculateDetailedProgress(
        actualFocusMinutes: Long,
        targetFocusMinutes: Long,
        completedTasks: Int,
        totalTasks: Int,
        completedTopics: Int,
        totalTopics: Int,
        weightTime: Int = 50,
        weightTask: Int = 30,
        weightTopic: Int = 20
    ): DetailedProgress {
        val timePct = if (targetFocusMinutes > 0) {
            (actualFocusMinutes.toDouble() / targetFocusMinutes.toDouble()) * 100.0
        } else null

        val taskPct = if (totalTasks > 0) {
            (completedTasks.toDouble() / totalTasks.toDouble()) * 100.0
        } else null

        val topicPct = if (totalTopics > 0) {
            (completedTopics.toDouble() / totalTopics.toDouble()) * 100.0
        } else null

        // Collect available components and their raw weights
        val availableComponents = mutableListOf<Pair<Double, Int>>()
        if (timePct != null) availableComponents.add(Pair(timePct, weightTime))
        if (taskPct != null) availableComponents.add(Pair(taskPct, weightTask))
        if (topicPct != null) availableComponents.add(Pair(topicPct, weightTopic))

        if (availableComponents.isEmpty()) {
            return DetailedProgress(
                timeProgressPercent = null,
                actualFocusMinutes = actualFocusMinutes,
                targetFocusMinutes = targetFocusMinutes,
                taskProgressPercent = null,
                completedTasksCount = completedTasks,
                totalTasksCount = totalTasks,
                topicProgressPercent = null,
                completedTopicsCount = completedTopics,
                totalTopicsCount = totalTopics,
                overallWeightedPercent = 0.0,
                formulaExplanation = "No targets, tasks, or topics configured yet."
            )
        }

        val totalAvailableWeight = availableComponents.sumOf { it.second }
        val overallPct = if (totalAvailableWeight > 0) {
            availableComponents.sumOf { (pct, weight) ->
                pct * (weight.toDouble() / totalAvailableWeight.toDouble())
            }
        } else 0.0

        val explanation = if (availableComponents.size == 3) {
            "Time: $weightTime% | Tasks: $weightTask% | Topics: $weightTopic%"
        } else {
            val normalizedParts = availableComponents.map { (_, weight) ->
                val norm = Math.round((weight.toDouble() / totalAvailableWeight.toDouble()) * 100)
                "$norm%"
            }
            "Re-normalized weights: ${normalizedParts.joinToString(" / ")} (missing metrics omitted)"
        }

        return DetailedProgress(
            timeProgressPercent = timePct,
            actualFocusMinutes = actualFocusMinutes,
            targetFocusMinutes = targetFocusMinutes,
            taskProgressPercent = taskPct,
            completedTasksCount = completedTasks,
            totalTasksCount = totalTasks,
            topicProgressPercent = topicPct,
            completedTopicsCount = completedTopics,
            totalTopicsCount = totalTopics,
            overallWeightedPercent = overallPct,
            formulaExplanation = explanation
        )
    }

    /**
     * Credit-weighted aggregation for multiple academic modules
     */
    fun calculateCreditWeightedProgress(
        moduleProgressList: List<Pair<Double, Double>> // Pair(moduleProgressPercent, moduleCredits)
    ): Double {
        if (moduleProgressList.isEmpty()) return 0.0
        val totalCredits = moduleProgressList.sumOf { it.second }
        return if (totalCredits > 0) {
            moduleProgressList.sumOf { (pct, credits) -> pct * (credits / totalCredits) }
        } else {
            moduleProgressList.map { it.first }.average()
        }
    }
}
