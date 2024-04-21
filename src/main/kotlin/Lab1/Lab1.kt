package Lab1.Lab1

import Lab1.ExponentialDistribution
import Lab1.PoissonDistribution
import Lab1.UniformDistribution
import org.apache.commons.math3.distribution.ChiSquaredDistribution
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

fun main() {
    println("\n\n\n-={ Равномерное распределение случайной переменной }=-\n")
    UniformDistribution.uniformDistribution(0.0, 100.0, 100)

    println("\n\n\n-={ Показательное распределение случайной переменной }=-\n")
    ExponentialDistribution.exponentialDistribution(0.35, 100)

    println("\n\n\n-={ Пуассоновское распределение случайной переменной }=-\n")
    PoissonDistribution.poissonDistribution(0.5, 10, 5)

    println("\n")
}

