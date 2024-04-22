package lab1.Lab1

import lab1.ExponentialDistribution
import lab1.PoissonDistribution
import lab1.UniformDistribution

fun main() {
    println("\n\n\n-={ Равномерное распределение случайной переменной }=-\n")
    UniformDistribution.uniformDistribution(0.0, 100.0, 100)

    println("\n\n\n-={ Показательное распределение случайной переменной }=-\n")
    ExponentialDistribution.exponentialDistribution(0.35, 100)

    println("\n\n\n-={ Пуассоновское распределение случайной переменной }=-\n")
    PoissonDistribution.poissonDistribution(0.5, 10, 5)

    println("\n")
}

