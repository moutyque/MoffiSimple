package small.app.moffi

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun `Test json format`(){
        val seat = Seat(
            id = "se_98389a55-da1b-4922-aefc-22d2c86ec980",
            entityType = "SEAT",
            createdAt = "2021-04-13T17:29:06.128Z",
            updatedAt = "2022-10-07T10:45:18.918Z",
            name = "43",
            fullname = "Desk2_ 43",
            position = 19,
            status = "ENABLED",
            mapInformation = MapInformation(
                id="mpi_se_98389a55-da1b-4922-aefc-22d2c86ec980",
                provider = "MOFFI",
                type = "SEAT",
                placeId = "pl_se_98389a55-da1b-4922-aefc-22d2c86ec980"
            ),
            allowRecurringEvents = true,
            favorite = false,
            hasServices = true

        )

        val ws = Workspace(
            id = "8a80819a78c535da0178cc473bca0efc",
            companyId = "2c91808277af2d160177b06b84740681",
            name = "TOTO",
            seats = listOf(seat)
        )
        val mv = MoffiViewModel(applicationContext)
        mv._seat.value = seat
        mv._workspace.value = ws

        val order = mv.toOrder(1680724102797L,null)

        val actual = mapper.readTree(mapper.writeValueAsString(order))
        val expected = mapper.readTree(File("/home/qmouty/AndroidStudioProjects/Moffi/app/sampledata/orderExample.json"))
        assertThat(actual).isEqualTo(expected)

    }
}