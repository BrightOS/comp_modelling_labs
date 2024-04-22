package lab3

import lab1.Support
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

const val N = 100000
const val P = 0.95
const val Z = 1.96
const val alpha = 1 - P
const val a = -2.0
const val b = 4.0
const val iAcc = 36.0

data class Data(
    var i: Int = 0,
    var x: Double = 0.0,
    var d: Boolean = false,
    var f: Double = 0.0
) {
    override fun toString() = "Испытание $i: x = $x, c = $d, f = $f"
}

fun main() {
    println("\n-={ Моделирование по методу Монте-Карло. N = $N }=-\n")
    val data = (1..N).map { Data() }

    repeat(data.size) { i ->
        val x = uniform(a, b)
        val d = d(x)
        data[i].apply {
            this.i = i
            this.x = x
            this.d = d
            this.f = if (d) f(x) else 0.0
        }
    }

    data.filter { it.d }.subList(0, min(10, data.count { it.d } - 1)).forEach {
        println(it)
    }

    println()

    val n = data.count { it.d }
    val fSum = data.sumOf { it.f }
    val iCalc = (b - a) * (fSum / n)

    val variance = data.sumOf { (it.x - iCalc).pow(2) / (n - 1) }
    val standardDeviation = sqrt(variance)

    val interval = Pair(
        first = iCalc - Z * standardDeviation / sqrt(n.toDouble()),
        second = iCalc + Z * standardDeviation / sqrt(n.toDouble())
    )

    println("I вычисленное (omega): ${String.format(Locale.US, "%.${Support.VALUE_ROUND}f", iCalc)}")
    println("I точное: $iAcc")
    println("Погрешность: ${String.format(Locale.US, "%.${Support.VALUE_ROUND}f", iAcc - iCalc)}")
    println("При доверительной вероятности $P по таблице ф-ий Ф(x) находим z = $Z")
    println("Уровень значимости alpha = ${String.format(Locale.US, "%.2f", alpha)}")

    println()
    println(
        "Оценка интеграла попала в доверительный интервал: [${
            String.format(Locale.US, "%.${Support.VALUE_ROUND}f", interval.first)
        }, ${
            String.format(Locale.US, "%.${Support.VALUE_ROUND}f", interval.second)
        }]"
    )
}

private fun uniform(a: Double, b: Double) = a + (b - a) * Random.nextDouble(0.0, 1.0)

fun f(x: Double) = 8 + 2 * x - x * x

fun d(x: Double) = x >= -2 && x <= 4