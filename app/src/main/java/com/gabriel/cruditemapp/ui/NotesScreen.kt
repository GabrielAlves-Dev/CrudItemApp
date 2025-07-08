package com.gabriel.cruditemapp.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriel.cruditemapp.data.Note
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun NotesScreen() {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val notes = remember { mutableStateListOf<Note>() }
    val notesCollection = Firebase.firestore.collection("notes")

    LaunchedEffect(Unit) {
        notesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FIRESTORE", "Listen failed.", error)
                Firebase.crashlytics.recordException(error) // Crashlytics log
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val newNotes = snapshot.documents.mapNotNull { document ->
                    document.toObject(Note::class.java)?.copy(id = document.id)
                }
                notes.clear()
                notes.addAll(newNotes)
            }
        }
    }

    if (showEditDialog && editingNote != null) {
        EditNoteDialog(
            note = editingNote!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedNote ->
                val updatedData = mapOf(
                    "title" to updatedNote.title,
                    "content" to updatedNote.content
                )
                Firebase.crashlytics.log("Atualizando a nota com ID: ${updatedNote.id}")
                notesCollection.document(updatedNote.id).update(updatedData)
                showEditDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Conteúdo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val newNoteId = notesCollection.document().id
                val note = Note(id = newNoteId, title = title, content = content)
                // Log de criação no Crashlytics
                Firebase.crashlytics.log("Adicionando nova nota com ID: $newNoteId")
                notesCollection.document(note.id).set(note)
                title = ""
                content = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adicionar Item")
        }

        // Botão para testar o Crashlytics
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                // Força um crash para testar a integração com o Crashlytics
                throw RuntimeException("Test Crash")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Forçar Crash de Teste")
        }


        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    onUpdateClick = {
                        editingNote = note
                        showEditDialog = true
                    },
                    onDeleteClick = {
                        Firebase.crashlytics.log("Deletando a nota com ID: ${note.id}")
                        notesCollection.document(note.id).delete()
                    }
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onUpdateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Title: ${note.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Description: ${note.content}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(onClick = onUpdateClick) {
                    Text("Update")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDeleteClick) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit
) {
    var newTitle by remember { mutableStateOf(note.title) }
    var newContent by remember { mutableStateOf(note.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Título") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("Conteúdo") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedNote = note.copy(title = newTitle, content = newContent)
                    onSave(updatedNote)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}