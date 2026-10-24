package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true, length = 320)
    var email: String = "",

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String = "",

    @Column(name = "display_name", nullable = false, length = 120)
    var displayName: String = "",

    @Column(nullable = false)
    var enabled: Boolean = true,
) : BaseEntity()
