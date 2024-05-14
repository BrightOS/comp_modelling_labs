package rgr

import de.vandermeer.asciitable.AT_Context
import de.vandermeer.asciitable.AsciiTable
import java.util.*
import kotlin.math.*
import kotlin.random.Random

fun main() {
    QueuingSystem()
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
            println(functionLambda(t) / lambda)
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

    init {
        println("Время начала смены: \t${startTime.toString().padStart(2, '0')}:00")
        println("Время окончания смены: \t${endTime.toString().padStart(2, '0')}:00")
        println("Рабочее время: \t\t\t${workTime} часов")

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

        println(AsciiTable(AT_Context().apply {
            width = 120
        }).apply {
            addRule()
            addRow("Событие", "Время события", "Клиентов в очереди")
            addRule()
            events.forEach {
                addRow(
                    when (it.type) {
                        EventType.ARRIVE -> "Пришёл клиент под номером ${it.clientId}"
                        EventType.LEAVE -> "Обслужен клиент под номером ${it.clientId}"
                    }, it.time.toLocalizedTime(startTime), it.queueSize
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
            clients.forEach {
                addRow(
                    it.id,
                    it.arrivalTime.toLocalizedTime(startTime),
                    it.leaveTime?.toLocalizedTime(startTime),
                    it.serviceTime.toLocalizedTime(0),
                    it.inQueueTime.toLocalizedTime(0),
                    it.inSystemTime.toLocalizedTime(0)
                )
            }
            addRule()
        }.render())


        println("\t\t\t-={X}=-\t-={X}=-\t-={X}=-")
        println("Оценки:")
        println("Количество клиентов за смену: \t\t${clients.size}")
        println("Время задержки закрытия: \t\t\t${countDelayAfterClosure().toLocalizedTime(0)}")
        println("Среднее время клиента в очереди: \t${clients.map { it.inQueueTime }.average().toLocalizedTime(0)}")
        println("Среднее время клиента в системе: \t${clients.map { it.inSystemTime }.average().toLocalizedTime(0)}")
        println(
            "Коэффициент занятости устройства: \t${
                (1 - (events.last().time - events[events.size - 2].time) / workTime).round(
                    ROUND_SIZE
                )
            }"
        )
        println("Средняя длина очереди: \t\t\t\t${events.map { it.queueSize }.average().round(ROUND_SIZE)}")
        println("\t\t\t-={X}=-\t-={X}=-\t-={X}=-")
    }
}