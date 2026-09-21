package com.example.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.LoadingView
import com.example.core.ui.components.ProjectCard
import com.example.core.ui.theme.AppSpacing
import com.example.feature.home.components.DeleteProjectDialog
import com.example.feature.home.components.NewProjectBottomSheet
import com.example.feature.home.components.RenameProjectDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNewProjectClick: () -> Unit,
    onDismissNewProjectSheet: () -> Unit,
    onCreateProject: (name: String, aspectRatio: AspectRatio) -> Unit,
    onProjectClick: (projectId: String) -> Unit,
    onProjectRenameClick: (project: Project) -> Unit,
    onConfirmRename: (newName: String) -> Unit,
    onDismissRename: () -> Unit,
    onProjectDuplicateClick: (projectId: String) -> Unit,
    onProjectDeleteClick: (project: Project) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onErrorDismiss: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onErrorDismiss()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigationBar(
                selectedIndex = selectedBottomNavIndex,
                onItemSelected = { selectedBottomNavIndex = it }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Header Gradient Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F52BA), // Sapphire Blue
                                Color(0xFF56CCF2), // Sky Blue
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Bar (Import & Search)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VidoPRO",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Video create", color = Color.White, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Get started",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp).background(Color.White.copy(alpha=0.3f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeActionCard(
                            title = "New video",
                            icon = Icons.Default.AddCircle,
                            modifier = Modifier.weight(1f).height(100.dp),
                            onClick = onNewProjectClick
                        )
                        HomeActionCard(
                            title = "Edit photo",
                            icon = Icons.Default.Image,
                            modifier = Modifier.weight(1f).height(100.dp),
                            onClick = { /* TODO */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Recent Projects Horizontal Scroll
                    if (uiState.projects.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.projects) { project ->
                                RecentProjectMiniCard(
                                    project = project,
                                    onClick = { onProjectClick(project.id) }
                                )
                            }
                        }
                    } else if (!uiState.isLoading) {
                        Text("No recent projects", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }

            // Quick Tools Grid
            QuickToolsGrid(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // New Project Dialog/Sheet
    if (uiState.isNewProjectSheetOpen) {
        NewProjectBottomSheet(
            onDismissRequest = onDismissNewProjectSheet,
            onCreateProject = onCreateProject
        )
    }
    if (uiState.projectToRename != null) {
        RenameProjectDialog(initialName = uiState.projectToRename.name, onConfirm = onConfirmRename, onDismiss = onDismissRename)
    }
    if (uiState.projectToDelete != null) {
        DeleteProjectDialog(projectName = uiState.projectToDelete.name, onConfirm = onConfirmDelete, onDismiss = onDismissDelete)
    }
}

@Composable
fun HomeActionCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun RecentProjectMiniCard(project: Project, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail or placeholder
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(30.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Duration overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = TimeUtils.formatDuration(project.durationMs),
                color = Color.White,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun QuickToolsGrid(modifier: Modifier = Modifier) {
    val tools = listOf(
        "AutoCut" to Icons.Default.ContentCut,
        "Retouch" to Icons.Default.Face,
        "AI generator" to Icons.Default.Star,
        "Photo tools" to Icons.Default.Build,
        "Frame capture" to Icons.Default.CameraAlt,
        "Auto enhance" to Icons.Default.AutoFixHigh,
        "Smart lighting" to Icons.Default.Lightbulb,
        "Auto captions" to Icons.Default.ClosedCaption,
        "AI poster" to Icons.Default.Image,
        "Remove bg" to Icons.Default.PersonRemove,
        "AI editor" to Icons.Default.Edit,
        "Space" to Icons.Default.Cloud,
        "Adjust speed" to Icons.Default.PlayArrow,
        "Marketing" to Icons.Default.ShoppingCart,
        "Audio tools" to Icons.Default.Headphones
    )

    Column(modifier = modifier) {
        // Use a grid-like layout by chunking since we are inside a verticalScroll
        tools.chunked(3).forEach { rowTools ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (tool in rowTools) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { }
                    ) {
                        Icon(
                            imageVector = tool.second,
                            contentDescription = tool.first,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tool.first,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                // Fill empty spaces if not multiple of 3
                repeat(3 - rowTools.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HomeBottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val items = listOf(
            "Edit" to Icons.Default.Edit,
            "Templates" to Icons.Default.Dashboard,
            "AI Lab" to Icons.Default.Star,
            "Projects" to Icons.Default.Folder,
            "Inbox" to Icons.Default.Notifications,
            "Me" to Icons.Default.Person
        )
        items.forEachIndexed { index, pair ->
            NavigationBarItem(
                icon = { Icon(pair.second, contentDescription = pair.first) },
                label = { Text(pair.first, fontSize = 10.sp) },
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}
