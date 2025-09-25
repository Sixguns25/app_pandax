package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialtiesManagementScreen(
    db: AppDatabase,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val specialties by db.specialtyDao().getAll().collectAsState(initial = emptyList())
    var newSpecialtyName by remember { mutableStateOf("") }
    var editingSpecialty by remember { mutableStateOf<Specialty?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Gestionar Especialidades", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Formulario para agregar/editar especialidad
        OutlinedTextField(
            value = editingSpecialty?.name ?: newSpecialtyName,
            onValueChange = {
                if (editingSpecialty != null) {
                    editingSpecialty = editingSpecialty?.copy(name = it)
                } else {
                    newSpecialtyName = it
                }
            },
            label = { Text(if (editingSpecialty != null) "Editar Especialidad" else "Nueva Especialidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    if (editingSpecialty != null) {
                        // Editar especialidad
                        val updatedSpecialty = editingSpecialty!!.copy(name = editingSpecialty!!.name.trim())
                        if (updatedSpecialty.name.isNotBlank()) {
                            db.specialtyDao().update(updatedSpecialty)
                            message = "Especialidad actualizada correctamente ✅"
                            editingSpecialty = null
                        } else {
                            message = "Error: El nombre no puede estar vacío"
                        }
                    } else {
                        // Agregar nueva especialidad
                        val specialtyName = newSpecialtyName.trim()
                        if (specialtyName.isNotBlank()) {
                            db.specialtyDao().insert(Specialty(name = specialtyName))
                            message = "Especialidad agregada correctamente ✅"
                            newSpecialtyName = ""
                        } else {
                            message = "Error: El nombre no puede estar vacío"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editingSpecialty != null) "Guardar Cambios" else "Agregar Especialidad")
        }

        if (editingSpecialty != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    editingSpecialty = null
                    newSpecialtyName = ""
                    message = null
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Cancelar Edición")
            }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de especialidades
        Text("Especialidades Existentes", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(specialties) { specialty ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(specialty.name)
                        Row {
                            Button(
                                onClick = { editingSpecialty = specialty },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Editar")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        // Verificar si la especialidad está asignada
                                        val assignedSpecialists = db.specialistDao()
                                            .getSpecialistsBySpecialtyId(specialty.id)
                                        if (assignedSpecialists.isEmpty()) {
                                            db.specialtyDao().delete(specialty)
                                            message = "Especialidad eliminada correctamente ✅"
                                        } else {
                                            message = "Error: No se puede eliminar, hay especialistas asignados"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Volver")
        }
    }
}