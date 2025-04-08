package com.purrytify.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.purrytify.mobile.LocalPoppinsFont

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Sharp.Home, "Home")
    object Library : BottomNavItem("library", Icons.Sharp.Menu, "Your Library")
    object Profile : BottomNavItem("profile", Icons.Sharp.Person, "Profile")
}

@Composable
fun BottomNavigation(navController: NavController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedItemIndex by remember { mutableStateOf(0) }
    val defaultColor = Color(0xFFB3B3B3)
    val selectedColor = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = currentRoute == item.route
            if (isSelected) {
                selectedItemIndex = index
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        selectedItemIndex = index
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) selectedColor else defaultColor
                )
                Text(
                    text = item.label,
                    style = TextStyle(
                        fontFamily = LocalPoppinsFont.current,
                        color = if (isSelected) selectedColor else defaultColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.Profile
    )
   BottomNavigation(navController = navController, items = items)
}
