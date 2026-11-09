package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.PsychologistRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.calendar.CalendarScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.patient.PatientDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainNavigationTab {
  PATIENTS,
  CALENDAR
}

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
  var currentTab by remember { mutableStateOf(MainNavigationTab.PATIENTS) }

  if (selectedPatientId == null) {
    Scaffold(
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
            selected = currentTab == MainNavigationTab.PATIENTS,
            onClick = { currentTab = MainNavigationTab.PATIENTS },
            icon = { Icon(Icons.Default.People, contentDescription = "Pacientes") },
            label = { Text("Pacientes") },
            modifier = Modifier.testTag("nav_patients_tab")
          )
          NavigationBarItem(
            selected = currentTab == MainNavigationTab.CALENDAR,
            onClick = { currentTab = MainNavigationTab.CALENDAR },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario") },
            label = { Text("Calendario") },
            modifier = Modifier.testTag("nav_calendar_tab")
          )
        }
      }
    ) { innerPadding ->
      Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
        when (currentTab) {
          MainNavigationTab.PATIENTS -> {
            DashboardScreen(
              viewModel = dashboardViewModel,
              onPatientSelected = { patientId ->
                selectedPatientId = patientId
              },
              onSignOut = {
                signOutUser(credentialManager, coroutineScope)
              }
            )
          }
          MainNavigationTab.CALENDAR -> {
            CalendarScreen(
              viewModel = dashboardViewModel,
              onPatientSelected = { patientId ->
                selectedPatientId = patientId
              }
            )
          }
        }
      }
    }
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
