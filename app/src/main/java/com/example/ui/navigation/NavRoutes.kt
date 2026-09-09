package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Focus : Screen("focus", "Focus")
    data object Goals : Screen("goals", "Goals")
    data object Analytics : Screen("analytics", "Analytics")
    data object More : Screen("more", "More")

    // Sub-screens
    data object WorkItemDetail : Screen("work_item_detail/{workItemId}", "Work Item") {
        fun createRoute(workItemId: Long) = "work_item_detail/$workItemId"
    }
    data object RecoveryPlan : Screen("recovery_plan", "Recovery Plan")
    data object WhatIf : Screen("what_if", "What-If Simulator")
    data object DailyLog : Screen("daily_log", "Daily Log")
    data object Calendar : Screen("calendar", "Calendar")
    data object PdfReports : Screen("pdf_reports", "PDF Reports")
    data object BackupRestore : Screen("backup_restore", "Backup & Restore")
    data object Trash : Screen("trash", "Trash & Recovery")
    data object Achievements : Screen("achievements", "Achievements & XP")
    data object AuditLog : Screen("audit_log", "Audit Log")
    data object Settings : Screen("settings", "Settings")
    data object Onboarding : Screen("onboarding", "Welcome to Trackaa")
}
