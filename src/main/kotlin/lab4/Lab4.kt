import de.vandermeer.asciitable.AsciiTable
import java.io.File
import java.util.Collections
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.measureTime

fun main() {
    val experiments = 50
    println("Experiments count: $experiments\n")

    val tests = listOf(
        "C11" to "50.txt",
        "C12" to "75.txt",
        "C13" to "100.txt"
    ).map {
        it.first to buildList {
            repeat(experiments) { _ ->
                add(
                    GeneticAlgorithm(
                        filename = it.second,
                        generations = 5,
                        populationLength = 4,
                        mutationRate = 1.0
                    )
                )
            }
        }
    }

    println(AsciiTable().apply {
        addRule()
        addRow("Тест", "Вершин", "Поколений", "Лучшая дистанция")
        addRule()
        tests.forEach { test ->
            println("Now running ${test.first} tests...")
            println(
                "Tests on ${test.first} dataset completed in: ${
                    measureTime {
                        test.second
                            .map { it.invoke() }
                            .minBy { it.bestDistance }
                            .let {
                                addRow(test.first, it.citiesCount, it.generations, it.bestDistance)
                            }
                    }
                }"
            )
        }
        println()
        addRule()
    }.render())
}

data class Road(
    val city1: City,
    val city2: City
) {
    val distance = sqrt((city1.x - city2.x).pow(2) + (city1.y - city2.y).pow(2) + (city1.z - city2.z).pow(2))
}

data class City(
    val cityId: Int,
    val x: Double,
    val y: Double,
    val z: Double
)

data class DataScores(
    val citiesCount: Int,
    val generations: Int,
    val bestDistance: Double
)

class GeneticAlgorithm(
    filename: String,
    private val generations: Int,
    private val populationLength: Int,
    private val mutationRate: Double
) {

    private val cities = getCitiesFromFile(filename)
    private val roads = calculateRoads()

    operator fun invoke(): DataScores {
        var population = createPopulation()

        var bestDistance = Double.POSITIVE_INFINITY
        repeat(generations - 1) {
            val parents = selection(population)
            val children = cross(parents)
            val mutant = if (Random.nextDouble(.0, 1.0) <= mutationRate)
                mutate(children)
            else
                null

            population = (population + children + (mutant?.let { listOf(it) } ?: emptyList()))
                .sortedBy { calculateRouteLength(it) }
                .take(populationLength)

            val currentBest = calculateRouteLength(population.first())
            if (currentBest < bestDistance) {
                bestDistance = currentBest
            }
        }

//        println("Вершин - ${cities.size}, поколений - $generations, длина лучшего маршрута - $bestDistance")
        return DataScores(
            citiesCount = cities.size,
            generations = generations,
            bestDistance = bestDistance
        )
    }

    private fun getCitiesFromFile(fileName: String) = buildList {
        File(fileName).forEachLine { line ->
            line.trim()
                .split(" ")
                .let {
                    add(
                        City(
                            cityId = it[0].toInt(),
                            x = it[1].toDouble(),
                            y = it[2].toDouble(),
                            z = it[3].toDouble()
                        )
                    )
                }
        }
    }

    private fun calculateRoads(): List<Road> =
        cities
            .map { i -> cities.map { i to it } }
            .flatten()
            .filter { (first, second) -> first != second }
            .map { Road(it.first, it.second) }

    private fun calculateRouteLength(route: List<City>) =
        buildList {
            repeat(route.size - 1) { index ->
                add(roads.find { it.city1 == route[index] && it.city2 == route[index + 1] })
            }
            add(roads.find { it.city1 == route[route.size - 1] && it.city2 == route[0] })
        }.fold(.0) { acc, road -> acc + (road?.distance ?: throw Exception("WHAT THE HEEEEEEELL")) }

    private fun createPopulation(): List<List<City>> {
        val population = mutableListOf<List<City>>()
        val start = cities.indices.random()

        for (pathNumber in start..<start + populationLength)
            population.add(buildList {
                repeat(cities.size) { delta ->
                    val cityNumber = (pathNumber + delta) % cities.size
                    cities.find { it.cityId == cityNumber }?.let { add(it) }
                }
            })

        return population
    }

    private fun selection(population: List<List<City>>): List<List<City>> {
        val top4 = population
            .sortedBy { calculateRouteLength(it) }
            .take(4)
        return (0..3).toList().shuffled().map { top4[it] }
    }

    private fun cross(parents: List<List<City>>): List<List<City>> {
        val citiesCount = parents[0].size
        val a = (0..<citiesCount - 2).random()
        val b = (a + 1..<citiesCount - 1).random()

        val children = List(4) { mutableSetOf<City>() }

        for (i in 0..<citiesCount) {
            if (i < a) {
                children[0].add(parents[0][i])
                children[1].add(parents[1][i])
                children[2].add(parents[2][i])
                children[3].add(parents[3][i])
            } else if (i <= b) {
                children[0].add(parents[1][i])
                children[1].add(parents[0][i])
                children[2].add(parents[3][i])
                children[3].add(parents[2][i])
            } else {
                for (j in a..<citiesCount) {
                    children[0].add(parents[0][j])
                    children[1].add(parents[1][j])
                    children[2].add(parents[2][j])
                    children[3].add(parents[3][j])
                    children[0].add(parents[1][j])
                    children[1].add(parents[0][j])
                    children[2].add(parents[3][j])
                    children[3].add(parents[2][j])
                }
                for (j in 0..<a) {
                    children[0].add(parents[1][j])
                    children[1].add(parents[0][j])
                    children[2].add(parents[3][j])
                    children[3].add(parents[2][j])
                }
            }
        }

        return children.map { it.toList() }
    }

    private fun mutate(children: List<List<City>>): List<City> {
        val citiesCount = children[0].size

        val first = (0..<citiesCount).random()
        var second: Int
        do {
            second = (0..<citiesCount).random()
        } while (first == second)

        val mutant = children.shuffled().first().toMutableList()
        Collections.swap(mutant, first, second)

        return mutant
    }
}