package lab1

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.numbers.combinatorics.Factorial
import java.util.*
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

object PoissonDistribution {
    fun poisson(lambda: Double): Int {
        val u = Random.nextDouble(0.0, 1.0)
        var p = exp(-lambda)
        var f = p
        var i = 0
        while (u >= f) {
            p = lambda * p / (i + 1)
            f += p
            i += 1
        }
        return i
    }

    fun poissonDistribution(lambda: Double, n: Int, m: Int) {
        val elements = arrayListOf<Int>().apply {
            repeat(n) {
                add(poisson(lambda))
            }
//            addAll(listOf(1,2,0,1,0,1,0,0,0,0))
        }

        println("Начальный список элементов: ${
            elements
                .joinToString(", ")
        }\n")

        val expectedFrequencies = arrayListOf<Double>()
        repeat(n) { i ->
            expectedFrequencies.add(
                exp(-lambda) * lambda.pow(i) * m / Factorial.value(i)
            )
        }

        println("Ожидаемые частоты: ${
            expectedFrequencies
                .map { String.format(Locale.US, "%.${Support.VALUE_ROUND}f", it) }
                .joinToString(", ")
        }\n")

        var chi = 0.0
        repeat(n) { i ->
            chi += (elements[i] - expectedFrequencies[i]).pow(2)
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