package com.patientcard.ai.context

import com.patientcard.ai.provider.PatientContext
import java.util.UUID

interface PatientContextProvider {
    fun buildContext(patientId: UUID, question: String): PatientContext
}
