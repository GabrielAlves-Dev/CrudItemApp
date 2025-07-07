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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun NotesScreen() {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    val notes = remember { mutableStateListOf<Note>() }

    val notesCollection = Firebase.firestore.collection("notes")

    LaunchedEffect(Unit) {
        notesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FIRESTORE", "Listen failed.", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val newNotes = snapshot.documents.map { document ->
                    document.toObject(Note::class.java)!!
                }
                notes.clear()
                notes.addAll(newNotes)
            }
        }
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
                notesCollection.document(newNoteId).set(note)
                title = ""
                content = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adicionar Item")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(notes) { note ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Title: ${note.title}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Description: ${note.content}", style = MaterialTheme.typography.bodyMedium)
                        Row {
                            Button(onClick = { /* Lógica de Update aqui */ }) {
                                Text("Update")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                notesCollection.document(note.id).delete()
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}