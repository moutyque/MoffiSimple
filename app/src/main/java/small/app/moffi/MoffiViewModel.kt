package small.app.moffi

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class MoffiViewModel(applicationContext: Context) : ViewModel() {
    private val repository = Repository(applicationContext)

    private val _buildings = mutableStateOf(emptyList<Building>())
    private val _cityName = mutableStateOf("")
    private val _workspaces = mutableStateOf(emptyList<Workspace>())
    internal val _workspace = mutableStateOf(Workspace("", "", "", emptyList()))
    private val _seats = mutableStateOf(emptyList<Seat>())
    private val _seatName = mutableStateOf("")
    internal val _seat = mutableStateOf(
        Seat()
    )
    val buildings: State<List<Building>> = _buildings
    val workspaces: State<List<Workspace>> = _workspaces
    val seats: State<List<Seat>> = _seats

    suspend fun getData(user: User) = repository.getData(user)
    fun onBuildingLoad(newBuildings: List<Building>) {
        Log.d("ViewModel", "Update buildings")
        _buildings.value = newBuildings
    }

    fun onBuildingSelection(newName: String) {
        Log.d("ViewModel", "Update selected building")
        _cityName.value = newName
        _workspaces.value =
            buildings.value.first { it.name == newName }.floors.flatMap { it.workspace }
    }

    fun onWorkspaceSelection(newName: String) {
        Log.d("ViewModel", "Update selected building")
        _workspace.value = workspaces.value.first { it.name == newName }
        _seats.value = _workspace.value.seats
    }

    fun onSeatSelection(newName: String) {
        Log.d("ViewModel", "Update selected building")
        _seat.value = seats.value.first { it.name == newName }
        _seatName.value = newName
    }

    fun canOrder(): Boolean = _workspace.value.id.isNotBlank() && _seat.value.name.isNotBlank()

    fun toOrder(selectedStartDateMillis: Long, selectedEndDateMillis: Long?): Order = Order(
        company = Id(_workspace.value.companyId),
        timeZone = "Europe/Paris",
        bookings = listOf(
            Booking(
                workspace = Id(_workspace.value.id),
                workspaceId = _workspace.value.id,
                start = selectedStartDateMillis.toDayStart(),
                end = selectedEndDateMillis?.let {
                    it.toDayEnd()
                } ?: run {
                    selectedStartDateMillis.toDayEnd()
                },
                bookedSeats = listOf(_seat.value),
                days = listOf(
                    Day(
                        day = selectedStartDateMillis.toDayDate(),
                        date = selectedStartDateMillis.toDayStart()
                    )
                )
            )
        )
    )

    fun reserve(order: Order) = repository.reserve(order)

}
private val rc33format =SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRANCE)
private val dateformat =SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

fun Long.toDayDate(): String = dateformat.format(
    Date.from(
        Instant.ofEpochMilli(
            this
        )
    )
)

fun Long.toDayStart() = "${this.toDayDate()}T05:30:00.000Z"
fun Long.toDayEnd() = "${this.toDayDate()}T20:00:00.000Z"
