package dev.ajvanegasv.kontio.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.backup.BackupViewModel
import dev.ajvanegasv.kontio.presentation.backup.components.BackupSettingsCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard

@Composable
fun ProfileScreen(
    backupViewModel: BackupViewModel,
    modifier: Modifier = Modifier
) {
    val backupState by backupViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 90.dp, bottom = 110.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Tarjeta de Perfil
        item {
            KontioGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Mi Cuenta Kontio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gestión local de finanzas personales",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Sección de Google Drive Backup (WhatsApp style)
        item {
            BackupSettingsCard(
                state = backupState,
                onConnectGoogle = { backupViewModel.connectGoogleAccount() },
                onDisconnectGoogle = { backupViewModel.disconnectGoogleAccount() },
                onBackupClick = { backupViewModel.performBackup() },
                onRestoreClick = { backupViewModel.restoreBackup() }
            )
        }
    }
}
