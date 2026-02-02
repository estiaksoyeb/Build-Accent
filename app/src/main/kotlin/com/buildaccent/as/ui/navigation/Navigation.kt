package com.buildaccent.`as`.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buildaccent.`as`.ui.welcome.WelcomeScreen
import com.buildaccent.`as`.ui.create.CreateLessonScreen
import com.buildaccent.`as`.ui.create.EditLessonScreen
import com.buildaccent.`as`.ui.home.HomeScreen
import com.buildaccent.`as`.ui.studio.LessonStudioScreen
import com.buildaccent.`as`.ui.settings.SettingsScreen

enum class AccentBuilderScreen {
    Welcome,
    Home,
    Lesson,
    Create,
    Edit,
    Settings
}

@Composable
fun AccentBuilderNavHost(
    startDestination: String = AccentBuilderScreen.Home.name,
    onOnboardingComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    fun popBackStack() {
        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = AccentBuilderScreen.Welcome.name) {
            WelcomeScreen(
                onFinish = {
                    onOnboardingComplete()
                    navController.navigate(AccentBuilderScreen.Home.name) {
                        popUpTo(AccentBuilderScreen.Welcome.name) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AccentBuilderScreen.Home.name) {
            HomeScreen(
                navigateToLesson = { lessonId ->
                    navController.navigate("${AccentBuilderScreen.Lesson.name}/$lessonId")
                },
                navigateToCreate = {
                    navController.navigate(AccentBuilderScreen.Create.name)
                },
                navigateToEdit = { lessonId ->
                    navController.navigate("${AccentBuilderScreen.Edit.name}/$lessonId")
                },
                navigateToSettings = {
                    navController.navigate(AccentBuilderScreen.Settings.name)
                },
                navigateToWelcome = {
                    navController.navigate(AccentBuilderScreen.Welcome.name)
                }
            )
        }

        composable(
            route = "${AccentBuilderScreen.Lesson.name}/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.IntType })
        ) {
            LessonStudioScreen(
                onBackClick = { popBackStack() }
            )
        }
        
        composable(
            route = "${AccentBuilderScreen.Edit.name}/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.IntType })
        ) {
            EditLessonScreen(
                onBackClick = { popBackStack() }
            )
        }

        composable(route = AccentBuilderScreen.Create.name) {
            CreateLessonScreen(
                onBackClick = { popBackStack() }
            )
        }
        
        composable(route = AccentBuilderScreen.Settings.name) {
            SettingsScreen(
                onBackClick = { popBackStack() }
            )
        }
    }
}
