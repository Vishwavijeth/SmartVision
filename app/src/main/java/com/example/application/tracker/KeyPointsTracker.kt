
package com.example.application.tracker

import androidx.annotation.VisibleForTesting
import com.example.application.data.KeyPoint
import com.example.application.data.Person
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class KeyPointsTracker(
    trackerConfig: TrackerConfig = TrackerConfig(
        keyPointsTrackerParams = KeyPointsTrackerParams()
    )
) : AbstractTracker(trackerConfig) {

    override fun computeSimilarity(persons: List<Person>): List<List<Float>> {
        if (persons.isEmpty() && tracks.isEmpty()) {
            return emptyList()
        }
        val simMatrix = mutableListOf<MutableList<Float>>()
        persons.forEach { person ->
            val row = mutableListOf<Float>()
            tracks.forEach { track ->
                val oksValue = oks(person, track.person)
                row.add(oksValue)
            }
            simMatrix.add(row)
        }
        return simMatrix
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun oks(person1: Person, person2: Person): Float {
        if (config.keyPointsTrackerParams == null) return 0f
        person2.keyPoints.let { keyPoints ->
            val boxArea = area(keyPoints) + 1e-6
            var oksTotal = 0f
            var numValidKeyPoints = 0

            person1.keyPoints.forEachIndexed { index, _ ->
                val poseKpt = person1.keyPoints[index]
                val trackKpt = person2.keyPoints[index]
                val threshold = config.keyPointsTrackerParams.keypointThreshold
                if (poseKpt.score < threshold || trackKpt.score < threshold) {
                    return@forEachIndexed
                }
                numValidKeyPoints += 1
                val dSquared: Float =
                    (poseKpt.coordinate.x - trackKpt.coordinate.x).pow(2) + (poseKpt.coordinate.y - trackKpt.coordinate.y).pow(
                        2
                    )
                val x = 2f * config.keyPointsTrackerParams.keypointFalloff[index]
                oksTotal += exp(-1f * dSquared / (2f * boxArea * x.pow(2))).toFloat()
            }
            if (numValidKeyPoints < config.keyPointsTrackerParams.minNumKeyPoints) {
                return 0f
            }
            return oksTotal / numValidKeyPoints
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun area(keyPoints: List<KeyPoint>): Float {
        val validKeypoint = keyPoints.filter {
            it.score > config.keyPointsTrackerParams?.keypointThreshold ?: 0f
        }
        if (validKeypoint.isEmpty()) return 0f
        val minX = min(1f, validKeypoint.minOf { it.coordinate.x })
        val maxX = max(0f, validKeypoint.maxOf { it.coordinate.x })
        val minY = min(1f, validKeypoint.minOf { it.coordinate.y })
        val maxY = max(0f, validKeypoint.maxOf { it.coordinate.y })
        return (maxX - minX) * (maxY - minY)
    }
}
