package com.example.tutorHub

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tutorHub.ViewModel.AuthViewModel
import com.example.tutorHub.ViewModel.NotificationViewModel
import com.example.tutorHub.ViewModel.QuestionViewModel
import com.example.tutorHub.ViewModel.StudentDashboardViewModel
import com.example.tutorHub.ViewModel.TeacherDashboardViewModel
import com.example.tutorHub.ui.theme.presentation.LoginScreen
import com.example.tutorHub.ui.theme.presentation.SignupScreen
import com.example.tutorHub.ui.theme.presentation.StudentDashboardScreen
import com.example.tutorHub.ui.theme.presentation.TeacherDashboardScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.tutorHub.ViewModel.TutorViewModel
import com.example.tutorHub.ui.theme.presentation.DomainSelectScreen
import com.example.tutorHub.ui.theme.presentation.TutorListScreen
import com.example.tutorHub.ui.theme.presentation.PostWrittenQuestionScreen
import com.example.tutorHub.ui.theme.presentation.PostToTutorScreen
import com.example.tutorHub.ui.theme.presentation.AllQuestionsScreen
import com.example.tutorHub.ui.theme.presentation.StudentQuestionSolutionsScreen
import com.example.tutorHub.ui.theme.presentation.StudentScheduleScreen
import com.example.tutorHub.ui.theme.presentation.TeacherRequestsScreen
import com.example.tutorHub.ui.theme.presentation.NotificationsScreen
import com.example.tutorHub.ui.theme.presentation.StudentSessionRequestDetailsScreen
import com.example.tutorHub.ui.theme.presentation.StudentRequestsScreen
import com.example.tutorHub.ui.theme.presentation.VideoSessionScreen
import com.example.tutorHub.ui.theme.presentation.TeacherSessionRequestDetailsScreen

@Composable
fun AppNavigation(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                navController,
                viewModel = viewModel<AuthViewModel>()
            )
        }
        composable("signup") {
            SignupScreen(
                navController,
                viewModel = viewModel<AuthViewModel>()
            )
        }
        composable("student_dashboard") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                val uid = user.uid
                StudentDashboardScreen(
                    navController,
                    viewModel = viewModel<StudentDashboardViewModel>(),
                    studentId = uid
                )
            }
        }
        composable("teacher_dashboard") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                val uid = user.uid
                TeacherDashboardScreen(
                    viewModel = viewModel<TeacherDashboardViewModel>(),
                    teacherId = uid,
                    navController = navController
                )
            }
        }
        // New routes
        composable("post_written") {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            PostWrittenQuestionScreen(
                navController = navController,
                viewModel = viewModel<QuestionViewModel>(),
                studentId = uid
            )
        }
        // Domain selection can be opened with an optional mode: "schedule" or "post"
        composable("select_domain") {
            DomainSelectScreen(
                navController = navController,
                viewModel = viewModel<TutorViewModel>(),
                mode = ""
            )
        }
        composable("select_domain/{mode}") { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: ""
            DomainSelectScreen(
                navController = navController,
                viewModel = viewModel<TutorViewModel>(),
                mode = mode
            )
        }
        composable("tutor_list/{domain}") { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain")?.let { Uri.decode(it) } ?: ""
            TutorListScreen(
                navController = navController,
                viewModel = viewModel<TutorViewModel>(),
                domain = domain,
                mode = ""
            )
        }
        composable("tutor_list/{domain}/{mode}") { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain")?.let { Uri.decode(it) } ?: ""
            val mode = backStackEntry.arguments?.getString("mode") ?: ""
            TutorListScreen(
                navController = navController,
                viewModel = viewModel<TutorViewModel>(),
                domain = domain,
                mode = mode
            )
        }
        composable("post_to_tutor/{tutorId}/{domain}") { backStackEntry ->
            val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
            val domain = backStackEntry.arguments?.getString("domain")?.let { Uri.decode(it) } ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            PostToTutorScreen(
                navController = navController,
                viewModel = viewModel<QuestionViewModel>(),
                studentId = uid,
                tutorId = tutorId,
                domain = domain
            )
        }
        composable("schedule/{tutorId}/{domain}") { backStackEntry ->
            val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
            val domain = backStackEntry.arguments?.getString("domain")?.let { Uri.decode(it) } ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            StudentScheduleScreen(
                navController = navController,
                studentId = uid,
                tutorId = tutorId,
                domain = domain
            )
        }
        composable("schedule/{tutorId}/{domain}/{tutorName}") { backStackEntry ->
            val tutorId = backStackEntry.arguments?.getString("tutorId") ?: ""
            val domain = backStackEntry.arguments?.getString("domain")?.let { Uri.decode(it) } ?: ""
            val tutorName = backStackEntry.arguments?.getString("tutorName")?.let { Uri.decode(it) } ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            StudentScheduleScreen(
                navController = navController,
                studentId = uid,
                tutorId = tutorId,
                domain = domain,
                tutorName = tutorName
            )
        }
        composable("teacher_requests/{teacherId}") { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            TeacherRequestsScreen(
                navController = navController,
                teacherId = teacherId
            )
        }
        composable("teacher_request_details/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            TeacherSessionRequestDetailsScreen(
                navController = navController,
                requestId = requestId,
                teacherId = uid,
                viewModel = viewModel()
            )
        }
        composable("home") {
            // Your home screen
            Text("Home Screen")
        }
        composable("all_questions/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "written"
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            AllQuestionsScreen(
                navController = navController,
                tutorId = uid,
                type = type,
                viewModel = viewModel()
            )
        }
        composable("student_solution_screen/{questionId}") { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            StudentQuestionSolutionsScreen(
                navController = navController,
                questionId = questionId,
                viewModel = viewModel()
            )
        }
        composable("notifications") {
            NotificationsScreen(
                viewModel = viewModel<NotificationViewModel>(),
                onNavigateToSessionRequest = { sessionId ->
                    navController.navigate("student_request_details/$sessionId")
                },
                onNavigateToSessionDetails = { sessionId ->
                    navController.navigate("student_request_details/$sessionId")
                }
            )
        }
        composable("student_request_details/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            StudentSessionRequestDetailsScreen(
                navController = navController,
                requestId = requestId,
                studentId = uid,
                viewModel = viewModel()
            )
        }
        composable("student_requests/{studentId}") { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            StudentRequestsScreen(
                navController = navController,
                studentId = studentId,
                viewModel = viewModel()
            )
        }
        composable("video_session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            VideoSessionScreen(
                navController = navController,
                sessionId = sessionId,
                currentUserId = uid
            )
        }
    }
}
