package small.app.moffi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*

import androidx.compose.material3.DateRangePicker

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import small.app.moffi.ui.theme.MoffiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainTheme(MoffiViewModel(applicationContext))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTheme(moffiViewModel: MoffiViewModel = viewModel()) {
    val state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input)
    MoffiTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                ConnectionRow(moffiViewModel)
                DateRow(state)
                Cities(moffiViewModel)
                Workspaces(moffiViewModel)
                Seats(moffiViewModel)
                Reservation(moffiViewModel, state)
            }

        }
    }
}

@Composable
fun ConnectionRow(moffiViewModel: MoffiViewModel = viewModel()) {

    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    Row {

        // Creating a variable to store toggle state
        var passwordVisible by remember { mutableStateOf(false) }


        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            placeholder = { Text("Username") },
            modifier = Modifier.weight(1.0F)
        )

        // Create a Text Field for giving password input
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            placeholder = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                // Localized description for accessibility services
                val description = if (passwordVisible) "Hide password" else "Show password"

                // Toggle button to hide or display password
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            modifier = Modifier.weight(1.0F)
        )
    }
    Row {
        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        kotlin.runCatching {
                            moffiViewModel.getData(User(username, password))
                        }.onFailure {
                            println(it)
                        }.onSuccess {
                            CoroutineScope(Dispatchers.Main).launch {
                                moffiViewModel.onBuildingLoad(it)
                            }
                        }
                    }

                }
            },
            modifier = Modifier.weight(0.3F)
        ) {
            Text(text = "Connection")
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRow(state: DateRangePickerState) {
    Row {
        DateRangePicker(state = state, modifier = Modifier.weight(1f), showModeToggle = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericMenu(label: String, onValueChange: (String) -> Unit, items: Iterable<Named>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            // The `menuAnchor` modifier must be passed to the text field for correctness.
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = onValueChange,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        selectedOptionText = item.name
                        onValueChange(item.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }

        }
    }
}

@Composable
fun Cities(moffiViewModel: MoffiViewModel = viewModel()) {
    GenericMenu("Cities", moffiViewModel::onBuildingSelection, moffiViewModel.buildings.value)
}

@Composable
fun Workspaces(moffiViewModel: MoffiViewModel = viewModel()) {
    GenericMenu("Workspace", moffiViewModel::onWorkspaceSelection, moffiViewModel.workspaces.value)
}

@Composable
fun Seats(moffiViewModel: MoffiViewModel = viewModel()) {
    GenericMenu("Seats", moffiViewModel::onSeatSelection, moffiViewModel.seats.value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reservation(moffiViewModel: MoffiViewModel = viewModel(), state: DateRangePickerState) {
    Row {
        Button(
            onClick = {
                if (moffiViewModel.canOrder() && null != state.selectedStartDateMillis) {
                    CoroutineScope(Dispatchers.IO).launch {
                        kotlin.runCatching {
                            state.selectedStartDateMillis?.let {

                                moffiViewModel.reserve(
                                    moffiViewModel.toOrder(it, state.selectedEndDateMillis)
                                )
                            }
                        }.onFailure {
                            println(it)
                        }.onSuccess {
                            when (it) {
                                is JsonNode -> {
                                    Log.i("Activity", "Reservation done")
                                    Log.i("Activity", it.toString())
                                }
                                is ResponseBody -> {
                                    Log.i("Activity", "Reservation failed")
                                    Log.i("Activity", it.string())
                                }
                                else -> {
                                    Log.i("Activity", "No answer")
                                }
                            }

                        }
                    }

                }
            },
            modifier = Modifier.weight(0.3F)
        ) {
            Text(text = "Reservation")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview(moffiViewModel: MoffiViewModel = viewModel()) {
    MainTheme(moffiViewModel)
}