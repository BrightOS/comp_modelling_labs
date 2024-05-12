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
    (this + startTime).let {
        "${
            this.round(ROUND_SIZE)
        } (${
            floor(it).roundToInt().toString().padStart(2, '0')
        }:${
            floor((it % 1.0) * 60).roundToInt().toString().padStart(2, '0')
        })"
    }

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
) {
    fun getLocalizedTime(startTime: Int) =
        time.toLocalizedTime(startTime)

}

enum class EventType {
    ARRIVE,
    LEAVE
}

class QueuingSystem {

    private var delayAfterClosure = 0.0  // время после T, когда уходит последний пациент
    private var currentlyArrived = 0  // количество прибывших пациентов к моменту времени t
    private var currentlyLeaved = 0  // количество уходов пациентов к моменту времени t
    private var currentQueryLength = 0  // количество пациентов к моменту времени t

    private val intensity = 2000.0  // интенсивность, количество пациентов в час
    private val lambda = 2.03  // интенсивоность появления событий

    private val events = arrayListOf<Event>()
    private val clients = arrayListOf<Client>()

    private var globalTime = 0.0
    private var lastArrived = 0.0
    private var lastLeaved = 0.0
    private var workTime = 0.0

    private val startTime = 9
    private val endTime = 23

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

    private fun functionLambda(t: Double) =
        lambdaMap[floor(t + startTime).roundToInt().let {
            "$it:00-${it + 1}:00"
        }] ?: 0.0

    private fun poissonProcess(s: Double, lambda: Double): Double {
        var t = s
        do {
            val u1 = Random.nextDouble(0.0, 1.0)
            t -= ln(u1) / lambda
            if (t > endTime - startTime)
                return t
            val u2 = Random.nextDouble(0.0, 1.0)
        } while (u2 > functionLambda(t) / lambda)
        return t
    }

    private fun firstClientIdInQueue() =
        events
            .groupBy { it.clientId }
            .filter { it.value.size != 2 }
            .let {
                if (it.size < 2)
                    return@let null
                else
                    it.map { pair -> pair.key }[1]
            }

    // Случайная переменная времени обслуживания клиента
    private fun exponentialProcess(lambda: Double) =
        -ln(Random.nextDouble(0.0, 1.0)) / lambda

    private fun clientArrived() {
        globalTime = lastArrived  // Ta - время прибытия пациента
        currentlyArrived += 1  // Na - количество прибывших пациентов (номер пациента)
        currentQueryLength += 1  // n - количество пациентов
        lastArrived = poissonProcess(globalTime, intensity)  // Ta = Tt время следующего прибытия пациента
        if (currentQueryLength == 1) {
            lastLeaved = globalTime + exponentialProcess(lambda)  // Td - время уход пациента
        }
        clients.add(Client(arrivalTime = globalTime))
        events.add(Event(currentlyArrived, EventType.ARRIVE, globalTime, currentQueryLength))
    }

    private fun clientLeaved() {

        globalTime = lastLeaved  // Td - время ухода пациента

        currentlyLeaved += 1  // Nd - количество уходов пациента
        currentQueryLength -= 1  // n - количество пациентов (уменьшается если уходит пациент)

        lastLeaved = if (currentQueryLength == 0)  // Если уходит последний пациент
            Double.MAX_VALUE  // Бесконечность
        else  // Если не уходит последний пациент
            globalTime + exponentialProcess(lambda)  // Td - время ухода пациента

        clients.find { it.id == currentlyLeaved }?.apply {
            leaveTime = globalTime

            firstClientIdInQueue()?.let { id ->
                clients.find { it.id == id }?.let {
                    it.inQueueTime = globalTime - it.arrivalTime
                }
            }

        }
        events.add(Event(currentlyLeaved, EventType.LEAVE, globalTime, currentQueryLength))
    }

    private fun countDelayAfterClosure() {
        delayAfterClosure = max(globalTime - workTime, .0)  // время после T, когда уходит последний пациент
    }

    init {
        workTime = (endTime - startTime).toDouble()
        println("Время начала смены: \t${startTime.toString().padStart(2, '0')}:00")  // Время начала смены
        println("Время окончания смены: \t${endTime.toString().padStart(2, '0')}:00")  // Время окончания смены
        println("Рабочее время: \t\t\t${workTime.roundToInt()} часов")  // Время работы

        lastArrived = exponentialProcess(lambda)  // tA = T0
        lastLeaved = Double.MAX_VALUE  // Бесконечность

        while (true) {
            when {
                (lastArrived <= lastLeaved) && (lastArrived <= workTime) -> clientArrived()
                (lastLeaved < lastArrived) && (lastLeaved <= workTime) -> clientLeaved()
                (minOf(lastArrived, lastLeaved) > workTime) && (currentQueryLength > 0) -> clientLeaved()
                (minOf(lastArrived, lastLeaved) > workTime) && (currentQueryLength == 0) -> {
                    countDelayAfterClosure()
                    break
                }
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
        println("Время задержки закрытия: \t\t\t${delayAfterClosure.toLocalizedTime(0)}")
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