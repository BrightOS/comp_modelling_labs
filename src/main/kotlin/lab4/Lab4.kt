package lab4

import kotlin.random.Random

fun main() {

}

data class City(
    val id: Int,
    val posX: Double,
    val posY: Double,
    val posZ: Double,
)

/**
 * Класс с решением задачи коммивояжера
 */
class TravelingSalesman {
    val populationSize = .0
    val generations = 0
    val mutationRate = .0
    val cities = mutableListOf<City>()
    val bestRoute = mutableListOf<City>()
    val calculateRouteLength = .0

    fun generateOffspring(parent1: List<City>, parent2: List<City>): MutableList<City> {
        val child = mutableListOf<City>()
        val shuffledIndexes = (0 until parent1.size).toList()
        shuffledIndexes.forEach { index ->
            child.add(parent1[index])
            child.add(parent2[index])
        }
        return child
    }

    fun mutate(individual: List<City>) = individual.shuffled()

    fun main() {
        val population = mutableListOf<MutableList<City>>()
        cities.forEach {
            population.add(cities.shuffled().toMutableList())
        }
        repeat(generations) {
            val num1 = (0 until population.size).random()
            val num2 = (0 until population.size).random()
            var child = mutableListOf<City>()
            if (num1 != num2)
                child = generateOffspring(population[num1], population[num2])
            if (Random.nextDouble(.0, 1.0) < mutationRate)
                child = mutate()
            population.add(child)
        }
    }
}