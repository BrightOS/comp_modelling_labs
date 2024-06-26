package rgr

import de.vandermeer.asciitable.AT_Context
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import java.util.*
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

fun main() {
    val dataScores = mutableListOf<DataScores>()

    dataScores.add(QueuingSystem().startWithOutput())
    repeat(89) {
        dataScores.add(QueuingSystem().start())
    }

    println(
        DataScores(
            clientsCount = -1,
            daysCount = dataScores.size,
            closingTimeDelay = dataScores.map { it.closingTimeDelay }.average(),
            averageQueueTime = dataScores.map { it.averageQueueTime }.average(),
            averageSystemTime = dataScores.map { it.averageSystemTime }.average(),
            averageOccupancyRate = dataScores.map { it.averageOccupancyRate }.average(),
            averageQueueLength = dataScores.map { it.averageQueueLength }.average(),
        )
    )
}

const val ROUND_SIZE = 5

fun Double.toLocalizedTime(startTime: Int) =
    if (!this.isNaN())
        (this + startTime).let {
            "${
                this.round(ROUND_SIZE)
            } (${
                floor(it).roundToInt().let {
                    val plusDays = it / 24
                    "${(it % 24).toString().padStart(2, '0')}${if (plusDays > 0) " + $plusDays д." else ""}"
                }
            }:${
                floor((it % 1.0) * 60).roundToInt().toString().padStart(2, '0')
            })"
        }
    else "NaN"

fun Double.round(decimals: Int) =
    String.format(
        Locale.ENGLISH,
        "%.${decimals}f",
        this
    )

fun getDayAddition(num: Int): String {
    val preLastDigit = num % 100 / 10

    if (preLastDigit == 1) {
        return "дней"
    }

    return when (num % 10) {
        1 -> "день"
        2, 3, 4 -> "дня"
        else -> "дней"
    }
}

data class Client(
    var id: Int = lastId++,
    val arrivalTime: Double,
    var leaveTime: Double? = null
) {
    val inSystemTime: Double
        get() = leaveTime?.minus(arrivalTime) ?: .0

    var inQueueTime: Double = .0

    val serviceTime: Double
        get() = inSystemTime - inQueueTime

    companion object {
        private var lastId = 1
        fun resetLastId() {
            lastId = 1
        }
    }
}

data class Event(
    val clientId: Int,
    val type: EventType,
    val time: Double,
    val queueSize: Int
)

enum class EventType {
    ARRIVE,
    LEAVE
}

data class DataScores(
    val clientsCount: Int,
    val daysCount: Int,
    val closingTimeDelay: Double,
    val averageQueueTime: Double,
    val averageSystemTime: Double,
    val averageOccupancyRate: Double,
    val averageQueueLength: Double
) {
//    override fun toString() =
//        StringBuilder()
//            .appendLine()
//            .appendLine("\t\t\t-={X}=-\t-={X}=-\t-={X}=-")
//            .appendLine("Оценки за $daysCount ${getDayAddition(daysCount)}:")
//            .append(if (clientsCount > 0) "Количество клиентов за смену: \t\t${clientsCount}\n" else "")
//            .appendLine("Время задержки закрытия: \t\t\t${closingTimeDelay.toLocalizedTime(0)}")
//            .appendLine("Среднее время клиента в очереди: \t${averageQueueTime.toLocalizedTime(0)}")
//            .appendLine("Среднее время клиента в системе: \t${averageSystemTime.toLocalizedTime(0)}")
//            .appendLine("Коэффициент занятости устройства: \t${averageOccupancyRate.round(ROUND_SIZE)}")
//            .appendLine("Средняя длина очереди: \t\t\t\t${averageQueueLength.round(ROUND_SIZE)}")
//            .toString()

    override fun toString() =
        AsciiTable().apply {
            addRule()
            addRow(null, "Оценки за $daysCount ${getDayAddition(daysCount)}:").apply {
                this.setTextAlignment(TextAlignment.CENTER)
            }
            addRule()
            if (clientsCount > 0)
                addRow("Количество клиентов за смену", clientsCount)
            addRow("Время задержки закрытия", closingTimeDelay.toLocalizedTime(0))
            addRow("Среднее время клиента в очереди", averageQueueTime.toLocalizedTime(0))
            addRow("Среднее время клиента в системе", averageSystemTime.toLocalizedTime(0))
            addRow("Коэффициент занятости устройства", averageOccupancyRate.round(ROUND_SIZE))
            addRow("Средняя длина очереди", averageQueueLength.round(ROUND_SIZE))
            addRule()
        }.render()
}

class QueuingSystem {

    private var currentlyArrived = 0    // Количество прибывших клиентов к моменту времени t
    private var currentlyLeaved = 0     // Количество обслуженных клиентов к моменту времени t
    private var currentQueueLength = 0  // Количество клиентов в очереди к моменту времени t

    private var lastArrived = 0.0   // Время последнего пришедшего клиента
    private var lastLeaved = 0.0    // Время последнего ушедшего клиента
    private var globalTime = 0.0    // Глобальное время всей системы

    private val intensity = 15.0    // Часовая стандартная интенсивность прихода клиентов
    private val lambda = 2.03       // Интенсивность времени обслуживания

    private val events = arrayListOf<Event>()
    private val clients = arrayListOf<Client>()

    private val startTime = 9
    private val endTime = 23
    private val workTime: Int
        get() = endTime - startTime

    /**
     * Карта интенсивности.
     */
    private val lambdaMap = mapOf(
        "9:00-10:00" to 20.2 / 60,
        "10:00-11:00" to 28.1 / 60,
        "11:00-12:00" to 24.6 / 60,
        "12:00-13:00" to 44.0 / 60,
        "13:00-14:00" to 40.8 / 60,
        "14:00-15:00" to 36.1 / 60,
        "15:00-16:00" to 28.5 / 60,
        "16:00-17:00" to 28.4 / 60,
        "17:00-18:00" to 52.9 / 60,
        "18:00-19:00" to 50.1 / 60,
        "19:00-20:00" to 48.6 / 60,
        "20:00-21:00" to 32.4 / 60,
        "21:00-22:00" to 36.3 / 60,
        "22:00-23:00" to 20.2 / 60
    )

    /**
     * Интервальная функция интенсивности.
     * @param t Время, для которого мы ищем интенсивность.
     * @return Интенсивность в конкретный час.
     */
    private fun functionLambda(t: Double) =
        lambdaMap[floor(t + startTime).roundToInt().let {
            "$it:00-${it + 1}:00"
        }] ?: 0.0

    /**
     * Пуассоновский процесс.
     * Необходим для генерации случайного времени прихода клиента.
     * @param s Время системы при вызове.
     * @param lambda Лямбда-параметр функции.
     * @return Время предположительного прихода следующего клиента.
     */
    private fun poissonProcess(s: Double, lambda: Double): Double {
        var t = s

        do {
            val u1 = Random.nextDouble(0.0, 1.0)
            t -= ln(u1) / lambda
            val u2 = Random.nextDouble(0.0, 1.0)
            println(functionLambda(t) / lambda) // Вероятность появления клиентов
        } while (
            u2 > functionLambda(t) / lambda // Проверка соответствия вероятности появления клиентов
            && t <= workTime                // Проверка выхода времени за рамки рабочего времени
        )

        return t
    }

    /**
     * Получение ID следующего стоящего в очереди клиента.
     * @return Если в очереди меньше 2 человек - null, иначе - id (int).
     */
    private fun nextClientInQueueId() =
        events
            .groupBy { it.clientId }        // Группируем события по клиентам
            .filter { it.value.size != 2 }  // Ищем клиентов в очереди
            .let {
                if (it.size < 2)    // Если в очереди меньше 2 человек,
                    return@let null // то ожидающих своей очереди человек нет
                else
                    it.map { pair -> pair.key }[1]
            }

    /**
     * Показательный процесс.
     * Необходим для генерации случайного времени обслуживания клиента.
     * @param lambda Лямбда-параметр функции.
     * @return Время обслуживания клиента.
     */
    private fun exponentialProcess(lambda: Double) =
        -ln(Random.nextDouble(0.0, 1.0)) / lambda

    /**
     * Ситуация, когда прибыл новый клиент.
     */
    private fun clientArrived() {
        globalTime = lastArrived

        currentlyArrived += 1   // Обслужено клиентов
        currentQueueLength += 1 // Текущая длина очереди

        lastArrived = poissonProcess(globalTime, intensity) // Время следующего предполагаемого прибытия клиента
        if (currentQueueLength == 1)
            lastLeaved = globalTime + exponentialProcess(lambda)  // Генерируем время ухода следующего клиента

        clients.add(Client(arrivalTime = globalTime))
        events.add(Event(currentlyArrived, EventType.ARRIVE, globalTime, currentQueueLength))
    }

    /**
     * Ситуация, когда клиент УЖЕ был обслужен.
     */
    private fun clientLeaved() {

        globalTime = lastLeaved

        currentlyLeaved += 1    // Обслужено клиентов
        currentQueueLength -= 1 // Текущая длина очереди

        lastLeaved =
            if (currentQueueLength == 0)    // Если обслужен последний клиент
                Double.MAX_VALUE
            else                            // Если в очереди ещё есть люди
                globalTime + exponentialProcess(lambda)

        clients.find { it.id == currentlyLeaved }?.apply {
            leaveTime = globalTime

            nextClientInQueueId()?.let { id ->
                clients.find { it.id == id }?.let {
                    it.inQueueTime = globalTime - it.arrivalTime
                }
            }

        }
        events.add(Event(currentlyLeaved, EventType.LEAVE, globalTime, currentQueueLength))
    }

    /**
     * Функция подсчёта времени после T,
     * когда обслужен последний клиент.
     */
    private fun countDelayAfterClosure() = max(globalTime - workTime, .0)

    fun startWithOutput() = start().let {
        println("Время начала смены: \t${startTime.toString().padStart(2, '0')}:00")
        println("Время окончания смены: \t${endTime.toString().padStart(2, '0')}:00")
        println("Рабочее время: \t\t\t${workTime} часов")

        println(AsciiTable(AT_Context().apply {
            width = 120
        }).apply {
            addRule()
            addRow("Событие", "Время события", "Клиентов в очереди")
            addRule()
            events.forEach { event ->
                addRow(
                    when (event.type) {
                        EventType.ARRIVE -> "Пришёл клиент под номером ${event.clientId}"
                        EventType.LEAVE -> "Обслужен клиент под номером ${event.clientId}"
                    }, event.time.toLocalizedTime(startTime), event.queueSize
                )
            }
            addRule()
        }.render())

        println(AsciiTable(AT_Context().apply {
            width = 150
        }).apply {
            addRule()
            addRow(
                "Номер клиента",
                "Время прибытия (Ai)",
                "Время ухода (Di)",
                "Время обслуживания (Vi)",
                "Время в очереди(Wi)",
                "Время в системе"
            )
            addRule()
            clients.forEach { client ->
                addRow(
                    client.id,
                    client.arrivalTime.toLocalizedTime(startTime),
                    client.leaveTime?.toLocalizedTime(startTime),
                    client.serviceTime.toLocalizedTime(0),
                    client.inQueueTime.toLocalizedTime(0),
                    client.inSystemTime.toLocalizedTime(0)
                )
            }
            addRule()
        }.render())


        println(it)
        it
    }

    fun start(): DataScores {
        Client.resetLastId()

        lastArrived = exponentialProcess(lambda) // Время прихода первого клиента через показательный процесс.
        lastLeaved = Double.MAX_VALUE

        while (true) {
            when {
                (lastArrived <= lastLeaved) && (lastArrived <= workTime) -> clientArrived()
                (lastLeaved < lastArrived) && (lastLeaved <= workTime) -> clientLeaved()
                (minOf(lastArrived, lastLeaved) > workTime) && (currentQueueLength > 0) -> clientLeaved()
                (minOf(lastArrived, lastLeaved) > workTime) && (currentQueueLength == 0) -> break
            }
        }

        return DataScores(
            clientsCount = clients.size,
            daysCount = 1,
            closingTimeDelay = countDelayAfterClosure(),
            averageQueueTime = clients.map { it.inQueueTime }.average(),
            averageSystemTime = clients.map { it.inSystemTime }.average(),
            averageOccupancyRate = (1 - (events.last().time - events[events.size - 2].time) / workTime),
            averageQueueLength = events.map { it.queueSize }.average()
        )
    }

}