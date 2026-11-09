package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.PsychologistRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.patient.PatientDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppContent()
        }
      }
    }
  }
}

@Composable
fun AppContent() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  if (currentUser == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser }
    )
  } else {
    val uid = currentUser!!.uid
    key(uid) {
      MainNavigation(currentUserId = uid)
    }
  }
}

@Composable
fun MainNavigation(currentUserId: String) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val databaseId = context.getString(R.string.firestore_database_id)
  val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
  val repository = remember(firestore) { PsychologistRepository(firestore) }

  val dashboardViewModel: DashboardViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        DashboardViewModel(repository, currentUserId)
      }
    }
  )

  var selectedPatientId by remember { mutableStateOf<String?>(null) }

  if (selectedPatientId == null) {
    DashboardScreen(
      viewModel = dashboardViewModel,
      onPatientSelected = { patientId ->
        selectedPatientId = patientId
      },
      onSignOut = {
        signOutUser(credentialManager, coroutineScope)
      }
    )
  } else {
    PatientDetailScreen(
      currentUserId = currentUserId,
      patientId = selectedPatientId!!,
      repository = repository,
      onNavigateBack = {
        selectedPatientId = null
      }
    )
  }
}
