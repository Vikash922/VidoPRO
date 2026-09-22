package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.core.ui.theme.AppSpacing
import com.example.feature.home.components.DeleteProjectDialog
import com.example.feature.home.components.NewProjectBottomSheet
import com.example.feature.home.components.RenameProjectDialog

/**
 * HomeScreen — Clean solid dark theme matching MainActivity contract and reference designs.
 * No gradients used.
 */
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
    val scrollState = rememberScrollState()
    val bgColor = Color(0xFF0F111A)
    val cardColor = Color(0xFF161925)
    val accentColor = Color.White
    val onAccentColor = Color(0xFF0A0D14)
    var selectedProjectForOptions by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        bottomBar = {
            HomeBottomNavigationBar(selectedIndex = 0, onItemSelected = {})
        }
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentColor, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("V", color = onAccentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("VixEdit", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp).clickable { onSettingsClick() }
                    )
                }
            }

            // Banner Card (Solid dark background, NO GRADIENT)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E2230))
                    .border(1.dp, Color(0xFF2C3448), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Turn Your\nIdeas Into\nStunning Videos",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Edit • Create • Share",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(accentColor, shape = CircleShape)
                        .clickable { onNewProjectClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go", tint = onAccentColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // New Project Button — Clean Solid White
                Button(
                    onClick = { onNewProjectClick() },
                    modifier = Modifier.weight(1.5f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = onAccentColor),
                    contentPadding = PaddingValues()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "New", tint = onAccentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Project", color = onAccentColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).height(56.dp).clickable { },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Templates", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).height(56.dp).clickable { },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("AI Auto Edit", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tools Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("See All", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ToolIconItem("Effects", Icons.Default.AutoFixHigh)
                ToolIconItem("Filters", Icons.Default.ColorLens)
                ToolIconItem("Text", Icons.Default.Title)
                ToolIconItem("Stickers", Icons.Default.EmojiEmotions)
                ToolIconItem("Overlay", Icons.Default.Layers)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // My Projects Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Projects", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("See All", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.projects.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.projects) { project ->
                        RecentProjectCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onLongClick = { selectedProjectForOptions = project },
                            onMoreClick = { selectedProjectForOptions = project }
                        )
                    }
                }
            } else if (!uiState.isLoading) {
                Text("No recent projects", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (selectedProjectForOptions != null) {
        val project = selectedProjectForOptions!!
        Dialog(onDismissRequest = { selectedProjectForOptions = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                border = BorderStroke(1.dp, Color(0xFF2C3448)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = project.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${TimeUtils.formatDuration(project.durationMs)} • 1080p",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    HorizontalDivider(color = Color(0xFF2C3448))
                    Spacer(Modifier.height(8.dp))

                    // Duplicate Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val id = project.id
                                selectedProjectForOptions = null
                                onProjectDuplicateClick(id)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("Duplicate Project", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Rename Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val p = project
                                selectedProjectForOptions = null
                                onProjectRenameClick(p)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("Rename Project", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Delete Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val p = project
                                selectedProjectForOptions = null
                                onProjectDeleteClick(p)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("Delete Project", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (uiState.isNewProjectSheetOpen) {
        NewProjectBottomSheet(
            onDismissRequest = onDismissNewProjectSheet,
            onCreateProject = onCreateProject
        )
    }

    if (uiState.projectToRename != null) {
        RenameProjectDialog(
            initialName = uiState.projectToRename.name,
            onConfirm = onConfirmRename,
            onDismiss = onDismissRename
        )
    }

    if (uiState.projectToDelete != null) {
        DeleteProjectDialog(
            projectName = uiState.projectToDelete.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}

@Composable
fun ToolIconItem(title: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161925))
                .border(1.dp, Color(0xFF272D35), RoundedCornerShape(16.dp))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF00D2FF), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbModel = project.thumbnailPath
    val imageRequest = remember(thumbModel) {
        if (thumbModel != null) {
            ImageRequest.Builder(context)
                .data(thumbModel)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build()
        } else null
    }

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E2230))
                .border(1.dp, Color(0xFF2C3448), RoundedCornerShape(16.dp))
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = project.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            // 3-dots button in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Project Options",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = project.name,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${TimeUtils.formatDuration(project.durationMs)} • 1080p",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun HomeBottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFF0F111A),
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            "Home" to Icons.Default.Home,
            "Projects" to Icons.Default.Folder,
            "Templates" to Icons.Default.VideoLibrary,
            "Profile" to Icons.Default.Person
        )
        items.forEachIndexed { index, pair ->
            val isSelected = selectedIndex == index
            val color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
            NavigationBarItem(
                icon = { Icon(pair.second, contentDescription = pair.first, tint = color, modifier = Modifier.size(22.dp)) },
                label = { Text(pair.first, fontSize = 10.sp, color = color) },
                selected = isSelected,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
