package dev.ajvanegasv.kontio.presentation.backup.components

import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.BackupState
import dev.ajvanegasv.kontio.presentation.backup.BackupUiState
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard

@Composable
fun BackupSettingsCard(
    state: BackupUiState,
    onConnectGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Encabezado
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DashboardIcons.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Google Drive Backup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Copia cifrada en la nube (Estilo WhatsApp)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Cuenta de Google Vinculada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cuenta de Google:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.connectedAccount ?: "No conectada",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.connectedAccount != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (state.connectedAccount == null) {
                    Button(
                        onClick = onConnectGoogle,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Vincular", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnectGoogle,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Cambiar", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Información del Último Backup
            val lastBackup = state.latestBackup
            val lastBackupText = if (lastBackup != null) {
                val sizeKb = (lastBackup.sizeBytes / 1024).coerceAtLeast(1)
                "Última copia: ${lastBackup.accountCount} cuentas, ${lastBackup.transactionCount} tx ($sizeKb KB)"
            } else {
                "Última copia: Ninguna realizada aún"
            }

            Text(
                text = lastBackupText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.statusMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.state is BackupState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (state.state as BackupState.Error).error,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botones de acción
            val isLoading = state.state is BackupState.InProgress || state.state is BackupState.Connecting

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (state.state is BackupState.InProgress) (state.state as BackupState.InProgress).stage else "Conectando...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Respaldar
                    Button(
                        onClick = onBackupClick,
                        modifier = Modifier.weight(1.2f).height(46.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = state.connectedAccount != null
                    ) {
                        Text(
                            text = "Hacer Copia Ahora",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Botón Restaurar
                    OutlinedButton(
                        onClick = onRestoreClick,
                        modifier = Modifier.weight(0.9f).height(46.dp),
                        shape = CircleShape,
                        enabled = state.connectedAccount != null
                    ) {
                        Text(
                            text = "Restaurar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
