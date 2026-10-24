package com.example.ar_project_1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.ui.common.ErrorBanner

@Composable
fun AuthScreen(
    container: AppContainer,
    onChangeServer: () -> Unit,
) {
    val viewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(container.repository) }
        },
    )
    val state = viewModel.state.value
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.HealthAndSafety,
            contentDescription = null,
            modifier = Modifier.height(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text("Karta Pacjenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Prywatne dane medyczne na Twoim serwerze",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                TabRow(selectedTabIndex = if (state.mode == AuthMode.LOGIN) 0 else 1) {
                    Tab(
                        selected = state.mode == AuthMode.LOGIN,
                        onClick = { viewModel.setMode(AuthMode.LOGIN) },
                        text = { Text("Logowanie") },
                    )
                    Tab(
                        selected = state.mode == AuthMode.REGISTER,
                        onClick = { viewModel.setMode(AuthMode.REGISTER) },
                        text = { Text("Rejestracja") },
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (state.mode == AuthMode.REGISTER) {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::setDisplayName,
                        label = { Text("Nazwa użytkownika") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.fieldErrors.containsKey("displayName"),
                        supportingText = state.fieldErrors["displayName"]?.let { { Text(it) } },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::setEmail,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = state.fieldErrors.containsKey("email"),
                    supportingText = state.fieldErrors["email"]?.let { { Text(it) } },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text("Hasło") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło",
                            )
                        }
                    },
                    isError = state.fieldErrors.containsKey("password"),
                    supportingText = state.fieldErrors["password"]?.let { { Text(it) } },
                )

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(state.error)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (state.mode == AuthMode.LOGIN) "Zaloguj się" else "Utwórz konto")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Serwer: ${com.example.ar_project_1.data.api.ServerUrl.display(container.sessionManager.current().serverUrl)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onChangeServer) { Text("Zmień") }
        }
        Text(
            "To projekt studencki, nie produkt medyczny.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}
