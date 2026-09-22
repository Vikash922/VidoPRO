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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.LoadingView
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VixEdit",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pro",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Hero Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2A2359),
                                Color(0xFF1E1A3C)
                            )
                        )
                    )
                    .clickable { onNewProjectClick() }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Turn Your\nIdeas Into\nStunning Videos",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Edit • Create • Share",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                // Floating Action Button in Hero
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Start", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNewProjectClick,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Project", fontWeight = FontWeight.Bold)
                }

                QuickActionButton(
                    title = "Templates",
                    icon = Icons.Default.Dashboard,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    title = "AI Auto Edit",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tools Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Text(text = "See All", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToolIconItem("Effects", Icons.Default.AutoFixHigh)
                ToolIconItem("Filters", Icons.Default.Brush)
                ToolIconItem("Text", Icons.Default.Title)
                ToolIconItem("Stickers", Icons.Default.Face)
                ToolIconItem("Overlay", Icons.Default.Layers)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Projects
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Text(text = "See All", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.projects.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.projects) { project ->
                        RecentProjectCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onMoreClick = { /* TODO show menu */ }
                        )
                    }
                }
            } else if (!uiState.isLoading) {
                Text("No recent projects", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            
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
fun QuickActionButton(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(56.dp).clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ToolIconItem(title: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecentProjectCard(project: Project, onClick: () -> Unit, onMoreClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(32.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${TimeUtils.formatDuration(project.durationMs)} • 1080p",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).clickable(onClick = onMoreClick)
            )
        }
    }
}

@Composable
fun HomeBottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
    ) {
        val items = listOf(
            "Home" to Icons.Default.Home,
            "Projects" to Icons.Default.Folder,
            "Templates" to Icons.Default.Dashboard,
            "Profile" to Icons.Default.Person
        )
        items.forEachIndexed { index, pair ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                icon = { 
                    Icon(
                        pair.second, 
                        contentDescription = pair.first,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                label = { 
                    Text(
                        pair.first, 
                        fontSize = 10.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                selected = isSelected,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
