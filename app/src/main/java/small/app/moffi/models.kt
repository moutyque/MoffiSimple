package small.app.moffi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val mapper = jacksonObjectMapper()


interface Named {
    val name: String
}

data class Floor(
    override val name: String,
    val level: Int,
    val workspace: List<Workspace> = emptyList()
) : Named

data class Building(
    override val name: String,
    val id: String,
    val companyId: String,
    val floors: List<Floor> = emptyList()
) : Named

data class Workspace(
    override val name: String,
    val id: String,
    val companyId: String,
    val seats: List<Seat> = emptyList(),
) : Named

data class Seat(

    val id: String="",
    val entityType: String="",
    val createdAt: String = "",
    val updatedAt: String = "",
    val disabledAt: String = "",
    override val name: String = "",
    val fullname: String = "",
    val position: Int=0,
    val status: String = "",
    val mapInformation: MapInformation = MapInformation(),
    val allowRecurringEvents: Boolean = true,
    val favorite: Boolean=false,
    val hasServices: Boolean = false,
    ) : Named

data class MapInformation(

    val id: String ="",
    val provider: String="",
    val type: String="",
    val placeId: String=""

)

data class Order(
    val company: Id,
    val timeZone: String,
    val coupon: String? = null,
    val bookings: List<Booking>,
    val origin: String = "WIDGET"
)

data class Booking(
    val id: String? = null,
    val workspace: Id,
    val workspaceId: String,
    val start: String,
    val end: String,
    val places: Int = 1,
    val isMonthlyBooking: Boolean = false,
    val coupon: String? = null,
    val period: String = "DAY",
    val bookedSeats: List<Seat>,
    val days: List<Day>,
    val bookNextToInfo: Id = Id(),
    val rrule: String? = null
)

data class Id(val id: String? = null)
data class Day(val day: String, val date: String, val period: String = "DAY")

