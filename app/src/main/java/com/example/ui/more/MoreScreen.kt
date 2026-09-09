package com.example.ui.more

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.entity.*
import com.example.data.model.*
import com.example.domain.calculations.DurationCalculator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(viewModel: MoreViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var targetDialog by remember { mutableStateOf<TargetPeriod?>(null) }
    var scheduleDialog by remember { mutableStateOf(false) }
    var taxonomyDialog by remember { mutableStateOf<String?>(null) }
    var settingsDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }
    var restorePasswordDialog by remember { mutableStateOf(false) }
    var trashDialog by remember { mutableStateOf(false) }
    var auditDialog by remember { mutableStateOf(false) }
    var achievementsDialog by remember { mutableStateOf(false) }
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var restorePayload by remember { mutableStateOf<String?>(null) }

    val createBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backupPayload.orEmpty()) }
        }
        backupPayload = null
    }
    val openBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            restorePayload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            restorePasswordDialog = !restorePayload.isNullOrBlank()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SYSTEM & TOOLS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }) },
        modifier = modifier.testTag("more_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            state.errorMessage?.let { msg -> item { MessageCard(msg, true) { viewModel.clearMessage() } } }
            state.successMessage?.let { msg -> item { MessageCard(msg, false) { viewModel.clearMessage() } } }

            item {
                SectionCard("PROFILE & MOMENTUM", Icons.Default.Star) {
                    Text("Level ${state.levelProgress.currentLevel}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${state.totalXp} XP · ${state.achievements.count { it.isUnlocked }}/${state.achievements.size} achievements unlocked")
                    LinearProgressIndicator(
                        progress = { (state.levelProgress.progressPercent / 100.0).toFloat().coerceIn(0f,1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    TextButton(onClick = { achievementsDialog = true }) { Text("View achievements") }
                }
            }

            item {
                SectionCard("TARGETS", Icons.Default.Flag) {
                    Text("Minimum / Goal / Stretch targets drive Home, Debt/Credit, forecasting and reports.", style = MaterialTheme.typography.bodySmall)
                    TargetPeriod.values().forEach { period ->
                        val target = state.targets.firstOrNull { it.scopeType == TargetScope.GLOBAL && it.periodType == period }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(period.name.lowercase().replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
                                Text(if (target == null) "Not configured" else "${DurationCalculator.formatMinutesHuman(target.minMinutes)} / ${DurationCalculator.formatMinutesHuman(target.goalMinutes)} / ${DurationCalculator.formatMinutesHuman(target.stretchMinutes)}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { targetDialog = period }) { Text(if(target==null) "Set" else "Edit") }
                        }
                    }
                }
            }

            item {
                SectionCard("WEEKLY CAPACITY", Icons.Default.DateRange) {
                    Text("Recovery plans and feasibility never allocate above these limits.", style = MaterialTheme.typography.bodySmall)
                    state.availability.sortedBy { it.dayOfWeek }.forEach { day ->
                        CapacityRow(day) { enabled, minutes -> viewModel.updateAvailability(day.dayOfWeek,enabled,minutes) }
                    }
                }
            }

            item {
                SectionCard("SCHEDULED FOCUS", Icons.Default.Notifications) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.scheduledFocus.size} upcoming")
                        Button(onClick = { scheduleDialog = true }) { Text("Schedule") }
                    }
                    if(state.scheduledFocus.isEmpty()) Text("No upcoming focus reminders.", style = MaterialTheme.typography.bodySmall)
                    state.scheduledFocus.take(6).forEach { item ->
                        val dt = Instant.ofEpochMilli(item.scheduledEpochMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Medium)
                                Text("${dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))} · ${item.durationMinutes}m", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteScheduledFocus(item) }) { Icon(Icons.Default.Delete, "Delete reminder") }
                        }
                    }
                    TextButton(onClick = { viewModel.exactAlarmSettingsIntent()?.let(context::startActivity) }) { Text("Exact alarm access") }
                }
            }

            item {
                SectionCard("FOCUS PROTECTION & PREFERENCES", Icons.Default.Settings) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Do Not Disturb protection", fontWeight = FontWeight.Medium)
                            Text(if(viewModel.isDndPermissionGranted()) "System access granted" else "System access not granted", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(state.isDndEnabled, { viewModel.setDndEnabled(it) })
                    }
                    TextButton(onClick = { context.startActivity(viewModel.dndSettingsIntent()) }) { Text("Open DND access settings") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeSetting.values().forEach { t -> FilterChip(state.themeSetting==t,{viewModel.setTheme(t)},{Text(t.name)}) }
                    }
                    Text("Streak threshold: ${DurationCalculator.formatMinutesHuman(state.preferences.streakThresholdMinutes)}", style = MaterialTheme.typography.bodySmall)
                    Text("Progress weights: ${state.preferences.timeProgressWeight}% time · ${state.preferences.taskProgressWeight}% tasks · ${state.preferences.topicProgressWeight}% topics", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { settingsDialog = true }) { Text("Advanced preferences") }
                }
            }

            item {
                SectionCard("CUSTOM TAXONOMY", Icons.Default.Add) {
                    Text("Work types: ${state.workItemTypes.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                    Text("Interruption reasons: ${state.interruptionReasons.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { taxonomyDialog="type" }) { Text("Add work type") }
                        OutlinedButton(onClick = { taxonomyDialog="reason" }) { Text("Add reason") }
                    }
                }
            }

            item {
                SectionCard("PERFORMANCE REPORTS", Icons.Default.Share) {
                    Text("Reports are generated from actual period data, configured targets, streak and Momentum metrics.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick={viewModel.generatePdfReport("Daily Performance Review","Today")},modifier=Modifier.weight(1f)){Text("Daily")}
                        OutlinedButton(onClick={viewModel.generatePdfReport("Weekly Performance Review","Current Week")},modifier=Modifier.weight(1f)){Text("Weekly")}
                        OutlinedButton(onClick={viewModel.generatePdfReport("Monthly Performance Review","Current Month")},modifier=Modifier.weight(1f)){Text("Monthly")}
                    }
                    state.lastGeneratedPdf?.let { file ->
                        Button(onClick = {
                            val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share Trackaa report"))
                        }) { Text("Share latest PDF") }
                    }
                }
            }

            item {
                SectionCard("ENCRYPTED BACKUP & RESTORE", Icons.Default.Lock) {
                    Text("Portable backups contain the complete workspace and are authenticated with AES-GCM.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick={backupDialog=true},modifier=Modifier.weight(1f)){Text("Create backup")}
                        OutlinedButton(onClick={openBackupFile.launch(arrayOf("application/json","text/plain","*/*"))},modifier=Modifier.weight(1f)){Text("Restore file")}
                    }
                }
            }

            item {
                SectionCard("DATA & HISTORY", Icons.Default.Info) {
                    ToolRow("Trash & 30-day recovery", "Restore soft-deleted workspace items") { viewModel.loadTrash(); trashDialog=true }
                    ToolRow("Audit history", "Review recorded edits/deletes/restores/revisions") { auditDialog=true }
                }
            }
        }
    }

    targetDialog?.let { period ->
        val existing=state.targets.firstOrNull{it.scopeType==TargetScope.GLOBAL&&it.periodType==period}
        TargetEditorDialog(period,existing,onDismiss={targetDialog=null}) { min,goal,stretch,deadline -> viewModel.saveGlobalTarget(period,min,goal,stretch,deadline);targetDialog=null }
    }
    if(scheduleDialog) ScheduleDialog({scheduleDialog=false}) { title,epoch,duration -> viewModel.scheduleFocus(title,epoch,duration);scheduleDialog=false }
    taxonomyDialog?.let { type -> SimpleNameDialog(if(type=="type")"New work type" else "New interruption reason",{taxonomyDialog=null}) { name -> if(type=="type")viewModel.addWorkItemType(name) else viewModel.addInterruptionReason(name);taxonomyDialog=null } }
    if(settingsDialog) AdvancedSettingsDialog(state.preferences,{settingsDialog=false},viewModel)
    if(backupDialog) PassphraseDialog("Create encrypted backup",{backupDialog=false}) { password ->
        viewModel.createEncryptedBackup(password) { payload -> backupPayload=payload; createBackupFile.launch("Trackaa_${System.currentTimeMillis()}.trackaa.json") }
        backupDialog=false
    }
    if(restorePasswordDialog) PassphraseDialog("Unlock backup",{restorePasswordDialog=false;restorePayload=null}) { password ->
        restorePayload?.let { viewModel.restoreFromBackup(it,password){} }; restorePasswordDialog=false; restorePayload=null
    }
    if(trashDialog) TrashDialog(state.trashItems,{trashDialog=false},viewModel)
    if(auditDialog) ListDialog("Audit history",state.auditEvents.map{"${it.actionType} · ${it.entityType} #${it.entityId}\n${it.note.orEmpty()}"}){auditDialog=false}
    if(achievementsDialog) ListDialog("Achievements",state.achievements.map{"${if(it.isUnlocked)"✓" else "○"} ${it.title} — ${it.description}"}){achievementsDialog=false}
}

@Composable private fun SectionCard(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,content:@Composable ColumnScope.()->Unit){
    Surface(shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().border(1.dp,MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(16.dp))){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(title,fontWeight=FontWeight.Bold,letterSpacing=.7.sp)}
            content()
        }
    }
}

@Composable private fun MessageCard(text:String,error:Boolean,onDismiss:()->Unit){
    Surface(color=if(error)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(12.dp)){
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(text,Modifier.weight(1f));IconButton(onClick=onDismiss){Icon(Icons.Default.Close,"Dismiss")}}
    }
}

@Composable private fun CapacityRow(day:AvailabilityEntity,onSave:(Boolean,Long)->Unit){
    var minutes by remember(day.capacityMinutes){mutableStateOf(day.capacityMinutes.toString())}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Text(DayOfWeek.of(day.dayOfWeek).name.take(3),Modifier.width(42.dp))
        Switch(day.isAvailable,{onSave(it,minutes.toLongOrNull()?:day.capacityMinutes)})
        OutlinedTextField(minutes,{minutes=it.filter(Char::isDigit)},label={Text("min")},enabled=day.isAvailable,singleLine=true,modifier=Modifier.weight(1f))
        TextButton(onClick={onSave(day.isAvailable,minutes.toLongOrNull()?:day.capacityMinutes)},enabled=day.isAvailable){Text("Save")}
    }
}

@Composable private fun ToolRow(title:String,subtitle:String,onClick:()->Unit){
    TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth()){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall)}}
}

@Composable private fun TargetEditorDialog(period:TargetPeriod,target:TargetEntity?,onDismiss:()->Unit,onSave:(Long,Long,Long,Long?)->Unit){
    var min by remember{mutableStateOf(((target?.minMinutes?:0)/60.0).toString())};var goal by remember{mutableStateOf(((target?.goalMinutes?:0)/60.0).toString())};var stretch by remember{mutableStateOf(((target?.stretchMinutes?:0)/60.0).toString())}
    var deadline by remember{mutableStateOf(target?.deadlineEpochMs?.let{Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}.orEmpty())}
    AlertDialog(onDismissRequest=onDismiss,title={Text("${period.name} target")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Enter hours. Values are saved as Minimum ≤ Goal ≤ Stretch.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(min,{min=it},label={Text("Minimum hours")});OutlinedTextField(goal,{goal=it},label={Text("Goal hours")});OutlinedTextField(stretch,{stretch=it},label={Text("Stretch hours")})
        if(period==TargetPeriod.DEADLINE)OutlinedTextField(deadline,{deadline=it},label={Text("Deadline yyyy-MM-dd HH:mm")})
    }},confirmButton={Button(onClick={
        fun m(v:String)=(v.toDoubleOrNull()?.times(60))?.toLong()?:0L
        val epoch=if(period==TargetPeriod.DEADLINE)runCatching{LocalDateTime.parse(deadline,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}.getOrNull() else null
        onSave(m(min),m(goal),m(stretch),epoch)
    }){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun ScheduleDialog(onDismiss:()->Unit,onSave:(String,Long,Long)->Unit){
    var title by remember{mutableStateOf("")};var whenText by remember{mutableStateOf("")};var duration by remember{mutableStateOf("60")};var error by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Schedule focus")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(title,{title=it},label={Text("Title")});OutlinedTextField(whenText,{whenText=it},label={Text("When yyyy-MM-dd HH:mm")});OutlinedTextField(duration,{duration=it.filter(Char::isDigit)},label={Text("Duration minutes")});if(error)Text("Enter a valid future date/time",color=MaterialTheme.colorScheme.error)
    }},confirmButton={Button(onClick={val epoch=runCatching{LocalDateTime.parse(whenText,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}.getOrNull();if(epoch==null||epoch<=System.currentTimeMillis())error=true else onSave(title,epoch,duration.toLongOrNull()?:60)}){Text("Schedule")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun SimpleNameDialog(title:String,onDismiss:()->Unit,onSave:(String)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(value,{value=it},label={Text("Name")})},confirmButton={Button(onClick={if(value.isNotBlank())onSave(value)}){Text("Add")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun PassphraseDialog(title:String,onDismiss:()->Unit,onSave:(String)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column{Text("Use at least 8 characters. The passphrase is not stored by Trackaa.",style=MaterialTheme.typography.bodySmall);OutlinedTextField(value,{value=it},label={Text("Passphrase")},singleLine=true)}},confirmButton={Button(onClick={if(value.length>=8)onSave(value)},enabled=value.length>=8){Text("Continue")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun AdvancedSettingsDialog(prefs:com.example.data.repository.UserPreferences,onDismiss:()->Unit,vm:MoreViewModel){
    var streak by remember{mutableStateOf(prefs.streakThresholdMinutes.toString())};var wt by remember{mutableStateOf(prefs.timeProgressWeight.toString())};var wk by remember{mutableStateOf(prefs.taskProgressWeight.toString())};var wp by remember{mutableStateOf(prefs.topicProgressWeight.toString())}
    var quiet by remember{mutableStateOf(prefs.quietHoursEnabled)};var qStart by remember{mutableStateOf("%02d:%02d".format(prefs.quietHoursStartHour,prefs.quietHoursStartMinute))};var qEnd by remember{mutableStateOf("%02d:%02d".format(prefs.quietHoursEndHour,prefs.quietHoursEndMinute))}
    var eod by remember{mutableStateOf(prefs.endOfDayReminderEnabled)};var eodTime by remember{mutableStateOf("%02d:%02d".format(prefs.endOfDayReviewHour,prefs.endOfDayReviewMinute))}
    fun hm(text:String):Pair<Int,Int>?=runCatching{text.split(":").let{it[0].toInt() to it[1].toInt()}}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text("Advanced preferences")},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{OutlinedTextField(streak,{streak=it.filter(Char::isDigit)},label={Text("Streak threshold (minutes)")})}
        item{Text("Progress weights (must total 100%)",fontWeight=FontWeight.SemiBold)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){OutlinedTextField(wt,{wt=it.filter(Char::isDigit)},label={Text("Time %")},modifier=Modifier.weight(1f));OutlinedTextField(wk,{wk=it.filter(Char::isDigit)},label={Text("Tasks %")},modifier=Modifier.weight(1f));OutlinedTextField(wp,{wp=it.filter(Char::isDigit)},label={Text("Topics %")},modifier=Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("Quiet hours");Switch(quiet,{quiet=it})}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){OutlinedTextField(qStart,{qStart=it},label={Text("Start HH:mm")},modifier=Modifier.weight(1f));OutlinedTextField(qEnd,{qEnd=it},label={Text("End HH:mm")},modifier=Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("End-of-day review reminder");Switch(eod,{eod=it})}}
        item{OutlinedTextField(eodTime,{eodTime=it},label={Text("Review time HH:mm")})}
    }},confirmButton={Button(onClick={vm.setStreakThreshold(streak.toLongOrNull()?:prefs.streakThresholdMinutes);vm.setProgressWeights(wt.toIntOrNull()?:50,wk.toIntOrNull()?:30,wp.toIntOrNull()?:20);val qs=hm(qStart);val qe=hm(qEnd);if(qs!=null&&qe!=null)vm.setQuietHours(quiet,qs.first,qs.second,qe.first,qe.second);hm(eodTime)?.let{vm.setEndOfDayReview(eod,it.first,it.second)};onDismiss()}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun TrashDialog(trash:TrashItems,onDismiss:()->Unit,vm:MoreViewModel){
    AlertDialog(onDismissRequest=onDismiss,title={Text("Trash & recovery")},text={LazyColumn(Modifier.heightIn(max=360.dp)){
        items(trash.tasks){x->TrashRow("Task: ${x.title}"){vm.restoreTask(x.id)}}
        items(trash.workItems){x->TrashRow("Work item: ${x.name}"){vm.restoreWorkItem(x.id)}}
        items(trash.goals){x->TrashRow("Goal: ${x.title}"){vm.restoreGoal(x.id)}}
        if(trash.tasks.isEmpty()&&trash.workItems.isEmpty()&&trash.goals.isEmpty()&&trash.topics.isEmpty()&&trash.sessions.isEmpty())item{Text("Trash is empty")}
    }},confirmButton={TextButton(onClick={vm.purgeTrash()}){Text("Purge all",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick=onDismiss){Text("Close")}})
}
@Composable private fun TrashRow(label:String,onRestore:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));TextButton(onClick=onRestore){Text("Restore")}}}

@Composable private fun ListDialog(title:String,rows:List<String>,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={LazyColumn(Modifier.heightIn(max=400.dp)){if(rows.isEmpty())item{Text("No entries yet")};items(rows){Text(it,Modifier.padding(vertical=6.dp))}}},confirmButton={TextButton(onClick=onDismiss){Text("Close")}})}
