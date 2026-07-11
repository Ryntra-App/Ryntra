package com.rinthy.shared.model

data class Dashboard(
    val account: Account,
    val projects: List<Project>,
    val organizations: List<Organization>,
)
