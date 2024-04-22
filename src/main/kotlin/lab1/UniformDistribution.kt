package lab1

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object UniformDistribution {

    private fun uniform(a: Double, b: Double) =
        a + (b - a) * Random.nextDouble(0.0, 1.0)

    fun uniformDistribution(a: Double, b: Double, n: Int) {

        val elements = arrayListOf<Double>().apply {
            repeat(n) {
                add(uniform(a, b))
            }
        }

        println(
            "Начальный список элементов: ${
                elements.map { String.format(Locale.US, "%.${Support.VALUE_ROUND}f", it) }.joinToString(", ")
            }\n"
        )

        val k = Math.floor(1 + Math.log(n.toDouble())).roundToInt()
        val intervalLength = (a + b) / k
        val intervals = mutableListOf<Pair<Double, Double>>().apply {
            var incr = a
            repeat(k) {
                add(Pair(incr, incr + intervalLength))
                incr += intervalLength
            }
        }

        println("Количество интервалов = $k")
        println("Длина интервала = ${String.format(Locale.US, "%.${Support.VALUE_ROUND}f", intervalLength)}")
        println("Интервалы: ${intervals.map { "[${it.first}, ${it.second})" }.joinToString(", ")}\n")

        val frequencies = ArrayList((1..k).map { 0L })
        repeat(n) { i ->
            repeat(k) { j ->
                if (elements[i] >= intervals[j].first && elements[i] < intervals[j].second)
                    frequencies[j]++
            }
        }

        println(
            "Частоты: ${
                frequencies
                    .joinToString(", ")
            }"
        )

        val expectedFrequencies = arrayListOf<Double>()
        repeat(k) { i ->
            expectedFrequencies.add((intervals[i].second - intervals[i].first) * n / (b - a))
        }

        println("Ожидаемые частоты: ${
            expectedFrequencies
                .map { String.format(Locale.US, "%.${Support.VALUE_ROUND}f", it) }
                .joinToString(", ")
        }\n")

        var chi = 0.0
        repeat(k) { i ->
            chi += (frequencies[i] - expectedFrequencies[i]).pow(2) / expectedFrequencies[i]
        }

        val chiStr = String.format(Locale.US, "%.${Support.VALUE_ROUND}f", chi)
        println("Наблюдаемое Хи^2 = $chiStr")

        val alpha = 0.05
        val s = n - 2

        val criticalChi = ChiSquaredDistribution(s.toDouble()).inverseCumulativeProbability(1 - alpha)
        val criticalChiStr = String.format(Locale.US, "%.${Support.VALUE_ROUND}f", criticalChi)
        println("Табличное Хи^2 = $criticalChiStr\n")

        println(
            if (chi < criticalChi)
                "Нулевая гипотеза принимается: $chiStr < $criticalChiStr"
            else
                "Нулевая гипотеза отвергается: $chiStr >= $criticalChiStr"
        )
    }
}