package com.example.ui.more

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DndPauseBehavior
import com.example.data.model.ThemeSetting
import com.example.domain.calculations.DurationCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MoreViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showBackupModal by remember { mutableStateOf(false) }
    var showRestoreModal by remember { mutableStateOf(false) }
    var showTrashModal by remember { mutableStateOf(false) }
    var showAuditModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM & TOOLS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("more_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Level & XP Progression Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL ${state.levelProgress.currentLevel} MASTER",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${state.totalXp} Total XP Accumulated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (state.levelProgress.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${state.levelProgress.progressPercent.toInt()}% towards Level ${state.levelProgress.currentLevel + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // PDF Performance Report Generator
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF PERFORMANCE REPORT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Generate professional executive performance summaries formatted for sharing or archiving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.generatePdfReport("Weekly Performance Review", "Current Week")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Generate Weekly PDF", fontSize = 11.sp)
                            }
                        }

                        state.lastGeneratedPdf?.let { pdfFile ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("PDF Ready", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(pdfFile.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    IconButton(
                                        onClick = {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                pdfFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Trackaa Report"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Encrypted Backup & Restore Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ENCRYPTED BACKUP & RESTORE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Export and import your entire workspace protected with AES-GCM encryption and PBKDF2 passphrase derivation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showBackupModal = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create Backup")
                            }
                            OutlinedButton(
                                onClick = { showRestoreModal = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restore Backup")
                            }
                        }
                    }
                }
            }

            // Quick Tool Rows: Trash, Audit Log, Demo Workspace
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        ToolRowItem(
                            icon = Icons.Default.Delete,
                            title = "Trash & 30-Day Recovery",
                            subtitle = "Restore soft-deleted items or purge immediately",
                            onClick = {
                                viewModel.loadTrash()
                                showTrashModal = true
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ToolRowItem(
                            icon = Icons.Default.Info,
                            title = "Immutable Audit Log",
                            subtitle = "Cryptographic audit trail of all workspace modifications",
                            onClick = { showAuditModal = true }
                        )
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ToolRowItem(
                            icon = Icons.Default.Add,
                            title = "Seed Sample Semester Workspace",
                            subtitle = "Loads DSA, Database Systems, topics and past focus sessions",
                            onClick = { viewModel.insertDemoWorkspace() }
                        )
                    }
                }
            }

            // Settings & Preferences Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "PREFERENCES & FOCUS PROTECTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Theme Mode Selector
                        Text("Display Theme:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeSetting.values().forEach { t ->
                                FilterChip(
                                    selected = state.themeSetting == t,
                                    onClick = { viewModel.setTheme(t) },
                                    label = { Text(t.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // DND Focus Protection Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Do Not Disturb Protection", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Silences alerts during active focus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.isDndEnabled,
                                onCheckedChange = { viewModel.setDndEnabled(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // DND Pause Behavior
                        Text("DND Behavior on Pause:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.dndPauseBehavior == DndPauseBehavior.SUSPEND_WHILE_PAUSED,
                                onClick = { viewModel.setDndPauseBehavior(DndPauseBehavior.SUSPEND_WHILE_PAUSED) },
                                label = { Text("Suspend on Pause", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = state.dndPauseBehavior == DndPauseBehavior.KEEP_ACTIVE,
                                onClick = { viewModel.setDndPauseBehavior(DndPauseBehavior.KEEP_ACTIVE) },
                                label = { Text("Keep Active", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }

    // BACKUP MODAL
    if (showBackupModal) {
        var passphrase by remember { mutableStateOf("") }
        var generatedPayload by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showBackupModal = false },
            title = { Text("Create Encrypted Backup") },
            text = {
                Column {
                    Text("Enter a secure passphrase to encrypt your backup:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (generatedPayload != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Backup Encrypted Successfully!", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text("Backup code generated and stored safely in application storage.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passphrase.isNotBlank()) {
                            viewModel.createEncryptedBackup(passphrase) { payload ->
                                generatedPayload = payload
                            }
                        }
                    }
                ) { Text("Encrypt & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupModal = false }) { Text("Close") }
            }
        )
    }

    // RESTORE MODAL
    if (showRestoreModal) {
        var passphrase by remember { mutableStateOf("") }
        var backupCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRestoreModal = false },
            title = { Text("Restore Encrypted Backup") },
            text = {
                Column {
                    Text("Enter the encrypted backup JSON and your passphrase:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupCode,
                        onValueChange = { backupCode = it },
                        label = { Text("Backup JSON Content") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupCode.isNotBlank() && passphrase.isNotBlank()) {
                            viewModel.restoreFromBackup(backupCode, passphrase) {
                                showRestoreModal = false
                            }
                        }
                    }
                ) { Text("Decrypt & Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreModal = false }) { Text("Cancel") }
            }
        )
    }

    // TRASH MODAL
    if (showTrashModal) {
        AlertDialog(
            onDismissRequest = { showTrashModal = false },
            title = { Text("Trash & 30-Day Recovery") },
            text = {
                val trash = state.trashItems
                val totalDeleted = trash.goals.size + trash.workItems.size + trash.topics.size + trash.tasks.size + trash.sessions.size

                Column(modifier = Modifier.heightIn(max = 350.dp)) {
                    if (totalDeleted == 0) {
                        Text("Trash is empty.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(trash.tasks) { task ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Task: ${task.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { viewModel.restoreTask(task.id) }) { Text("Restore") }
                                }
                            }
                            items(trash.workItems) { w ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Module: ${w.name}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { viewModel.restoreWorkItem(w.id) }) { Text("Restore") }
                                }
                            }
                            items(trash.goals) { g ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Goal: ${g.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { viewModel.restoreGoal(g.id) }) { Text("Restore") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.purgeTrash() }) { Text("Purge All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showTrashModal = false }) { Text("Close") }
            }
        )
    }

    // AUDIT LOG MODAL
    if (showAuditModal) {
        AlertDialog(
            onDismissRequest = { showAuditModal = false },
            title = { Text("Immutable Audit Trail") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.auditEvents) { audit ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(audit.actionType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(audit.entityType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            audit.note?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditModal = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ToolRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
