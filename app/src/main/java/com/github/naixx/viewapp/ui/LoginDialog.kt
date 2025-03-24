package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.naixx.viewapp.BuildConfig

@Composable
fun LoginDialog(
    onLogin: (String, String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var openDialog by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf(BuildConfig.EMAIL) }
    var password by remember { mutableStateOf(BuildConfig.PASSWORD) }
    var isLoading by remember { mutableStateOf(false) }

    if (openDialog)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Login Required") },
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (isLoading) {
                        Text("Logging in...", modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        onLogin(username, password)
                    },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                Button(
                    onClick = { openDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
}
