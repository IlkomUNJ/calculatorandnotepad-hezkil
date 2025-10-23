package com.example.notepad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notepad.ui.theme.NotepadTheme

private const val PREFS_NAME = "NotepadPrefs"
private const val KEY_NOTE_TEXT = "noteText"

fun saveNote(context: Context, text: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NOTE_TEXT, text)
        .apply()
}

fun loadNote(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_NOTE_TEXT, "") ?: ""
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                NotepadScreenCompose()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreenCompose() {
    var noteTextState by remember { mutableStateOf(TextFieldValue("")) }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        val savedText = loadNote(context)
        noteTextState = TextFieldValue(savedText)
        if (savedText.isNotEmpty()) {
            showToast("Loaded last saved note.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveNote(context, noteTextState.text)
        }
    }

    fun getSelectedOrAllText(state: TextFieldValue): String {
        return if (!state.selection.collapsed) {
            state.text.substring(state.selection.start, state.selection.end)
        } else {
            state.text
        }
    }

    fun deleteSelectedOrAllText(state: TextFieldValue): TextFieldValue {
        return if (!state.selection.collapsed) {
            val newText = state.text.removeRange(state.selection.start, state.selection.end)
            TextFieldValue(newText, TextRange(state.selection.start))
        } else {
            TextFieldValue("", TextRange.Zero)
        }
    }


    Scaffold(
        topBar = {
            Text(
                text = "My Compose Notepad",
                modifier = Modifier.padding(16.dp)
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {
                        saveNote(context, noteTextState.text)
                        showToast("Note Saved to disk!")
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Note")
                    }

                    IconButton(onClick = {
                        val textToCopy = getSelectedOrAllText(noteTextState)
                        if (textToCopy.isNotEmpty()) {
                            val clip = ClipData.newPlainText("Note", textToCopy)
                            clipboardManager.setPrimaryClip(clip)
                            showToast("Text Copied!")
                        } else {
                            showToast("Nothing to copy.")
                        }
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Text")
                    }

                    IconButton(onClick = {
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val pasteText = clipData.getItemAt(0).coerceToText(context).toString()

                            val currentText = noteTextState.text
                            val selection = noteTextState.selection

                            val newText = currentText.substring(0, selection.start) +
                                    pasteText +
                                    currentText.substring(selection.end)

                            val newCursorPosition = selection.start + pasteText.length
                            noteTextState = TextFieldValue(newText, TextRange(newCursorPosition))
                            showToast("Text Pasted!")
                        } else {
                            showToast("Clipboard is empty.")
                        }
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste Text")
                    }

                    IconButton(onClick = {
                        val textToCut = getSelectedOrAllText(noteTextState)
                        if (textToCut.isNotEmpty()) {
                            val clip = ClipData.newPlainText("Note", textToCut)
                            clipboardManager.setPrimaryClip(clip)

                            noteTextState = deleteSelectedOrAllText(noteTextState)
                            showToast("Text Cut!")
                        } else {
                            showToast("Nothing to cut.")
                        }
                    }) {
                        Icon(Icons.Filled.ContentCut, contentDescription = "Cut Text")
                    }

                    IconButton(onClick = {
                        noteTextState = TextFieldValue("", TextRange.Zero)
                        showToast("New Note started.")
                    }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Note")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            noteTextState = TextFieldValue("", TextRange.Zero)
                            showToast("New Note started.")
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Filled.Add, "New Note")
                    }
                }
            )
        }
    ) { paddingValues ->
        TextField(
            value = noteTextState,
            onValueChange = { newTextFieldValue ->
                noteTextState = newTextFieldValue
            },
            label = { Text("Start typing your note here...") },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
@Preview
@Composable
fun liatNotepaPreview(){
    NotepadTheme {
        NotepadScreenCompose()
    }
}