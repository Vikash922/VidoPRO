package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.feature.home.components.DeleteProjectDialog
import com.example.feature.home.components.NewProjectBottomSheet
import com.example.feature.home.components.RenameProjectDialog

// Theme Palette matching Reference Design
private val BackgroundColor = Color(0xFFF8FAFF)
private val PrimaryBlue = Color(0xFF4A90C8)
private val DeepNavy = Color(0xFF1A3A5C)
private val BorderBlue = Color(0xFFDCEBF7)
private val SecondaryBlue = Color(0xFF8AB4D4)
private val MutedBlue = Color(0xFFAAC4DC)
private val InactiveNav = Color(0xFFB8CFE4)
private val DividerColor = Color(0xFFE8F1F9)
private val CardBackground = Color(0xFFFFFFFF)

data class TemplateItem(
    val id: Int,
    val name: String,
    val duration: String,
    val imgUrl: String,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
)

val defaultTemplates = listOf(
    TemplateItem(
        id = 1,
        name = "Cinematic",
        duration = "00:15",
        imgUrl = "https://images.pexels.com/photos/14169534/pexels-photo-14169534.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_16_9
    ),
    TemplateItem(
        id = 2,
        name = "Travel",
        duration = "00:30",
        imgUrl = "https://images.pexels.com/photos/3863218/pexels-photo-3863218.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_9_16
    ),
    TemplateItem(
        id = 3,
        name = "Sunset",
        duration = "00:20",
        imgUrl = "https://images.pexels.com/photos/29857601/pexels-photo-29857601.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_9_16
    ),
    TemplateItem(
        id = 4,
        name = "Vlog",
        duration = "01:00",
        imgUrl = "https://images.pexels.com/photos/28492053/pexels-photo-28492053.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_16_9
    ),
    TemplateItem(
        id = 5,
        name = "Reels",
        duration = "00:25",
        imgUrl = "https://images.pexels.com/photos/19779565/pexels-photo-19779565.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_9_16
    ),
    TemplateItem(
        id = 6,
        name = "Minimal",
        duration = "00:18",
        imgUrl = "https://images.pexels.com/photos/20344919/pexels-photo-20344919.jpeg?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=320&w=240",
        aspectRatio = AspectRatio.RATIO_1_1
    )
)

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * HomeScreen — Converted directly from the reference mobile design to native Jetpack Compose.
 * Features:
 * - Brand header with Zap icon, VidoPRO logo, Search and Settings
 * - Modern full-width "New Project" card button with blue glow
 * - 4 Quick Actions: AutoCut, Camera, AI Tools, Templates
 * - Segmented tabs for "Projects" and "Templates" with animated indicator
 * - Real live project list with video frame thumbnails, formatted duration, resolution & relative timestamps
 * - Project options dialog for Rename, Duplicate, Delete
 * - 2-column video templates showcase grid
 * - Bottom Navigation Bar (Home, Edit, Template, Inbox, Me)
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
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var activeNav by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableStateOf("projects") } // "projects" or "templates"
    var selectedProjectForOptions by remember { mutableStateOf<Project?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSortAscending by remember { mutableStateOf(false) }

    val quickActions = remember {
        listOf(
            QuickAction(label = "AutoCut", icon = Icons.Default.AutoFixHigh) { onNewProjectClick() },
            QuickAction(label = "Camera", icon = Icons.Default.PhotoCamera) { onNewProjectClick() },
            QuickAction(label = "AI Tools", icon = Icons.Default.AutoAwesome) { onNewProjectClick() },
            QuickAction(label = "Templates", icon = Icons.Default.Dashboard) { activeTab = "templates" }
        )
    }

    val displayProjects = remember(uiState.projects, searchQuery, isSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            uiState.projects
        } else {
            uiState.projects.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
        if (isSortAscending) {
            filtered.sortedBy { it.updatedAt }
        } else {
            filtered.sortedByDescending { it.updatedAt }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor),
        containerColor = BackgroundColor,
        bottomBar = {
            HomeBottomNavigationBar(
                selectedIndex = activeNav,
                onItemSelected = { index ->
                    activeNav = index
                    when (index) {
                        0 -> activeTab = "projects"
                        1 -> onNewProjectClick()
                        2 -> activeTab = "templates"
                        3 -> { /* Inbox */ }
                        4 -> onSettingsClick()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header: Zap icon + VidoPRO Title + Search & Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "VidoPRO",
                        color = DeepNavy,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DeepNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = DeepNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Optional Search Input Bar
            if (isSearchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        placeholder = { Text("Search projects...", color = SecondaryBlue, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderBlue,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = DeepNavy,
                            unfocusedTextColor = DeepNavy
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = SecondaryBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // New Project Button (Prominent Blue Pill)
                Button(
                    onClick = onNewProjectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .height(52.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = PrimaryBlue.copy(alpha = 0.35f),
                            spotColor = PrimaryBlue.copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Project",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Actions (AutoCut, Camera, AI Tools, Templates)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickActions.forEach { action ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(BorderStroke(1.5.dp, BorderBlue), RoundedCornerShape(14.dp))
                                .clickable { action.onClick() }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = action.label,
                                color = DeepNavy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Segmented Tabs: Projects vs Templates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Projects Tab Header
                        Column(
                            modifier = Modifier.clickable { activeTab = "projects" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Projects",
                                color = if (activeTab == "projects") DeepNavy else SecondaryBlue,
                                fontSize = 15.sp,
                                fontWeight = if (activeTab == "projects") FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (activeTab == "projects") PrimaryBlue else Color.Transparent)
                            )
                        }

                        // Templates Tab Header
                        Column(
                            modifier = Modifier.clickable { activeTab = "templates" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Templates",
                                color = if (activeTab == "templates") DeepNavy else SecondaryBlue,
                                fontSize = 15.sp,
                                fontWeight = if (activeTab == "templates") FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (activeTab == "templates") PrimaryBlue else Color.Transparent)
                            )
                        }
                    }

                    // Sort / Align Toggle Icon
                    IconButton(
                        onClick = { isSortAscending = !isSortAscending },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Content: Projects List or Templates Grid
                if (activeTab == "projects") {
                    if (displayProjects.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayProjects.forEach { project ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onProjectClick(project.id) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail preview
                                    Box(
                                        modifier = Modifier
                                            .size(width = 76.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFE8F2FC)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val imageRequest = remember(project.thumbnailPath) {
                                            if (project.thumbnailPath != null) {
                                                ImageRequest.Builder(context)
                                                    .data(project.thumbnailPath)
                                                    .decoderFactory(VideoFrameDecoder.Factory())
                                                    .crossfade(true)
                                                    .build()
                                            } else null
                                        }
                                        if (imageRequest != null) {
                                            AsyncImage(
                                                model = imageRequest,
                                                contentDescription = project.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MovieCreation,
                                                contentDescription = null,
                                                tint = SecondaryBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Project details
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = project.name,
                                            color = DeepNavy,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${TimeUtils.formatDuration(project.durationMs)} · ${project.aspectRatio.label}",
                                            color = SecondaryBlue,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = TimeUtils.formatLastEdited(project.updatedAt),
                                            color = MutedBlue,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // More options button
                                    IconButton(
                                        onClick = { selectedProjectForOptions = project },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = SecondaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 1.dp,
                                    color = DividerColor
                                )
                            }
                        }
                    } else {
                        // Empty State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = SecondaryBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No projects match \"$searchQuery\"" else "No projects yet",
                                color = DeepNavy,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ New Project' to create your first video",
                                color = SecondaryBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Templates Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            defaultTemplates.take(2).forEach { t ->
                                TemplateCard(
                                    template = t,
                                    onClick = {
                                        onCreateProject("${t.name} Edit", t.aspectRatio)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            defaultTemplates.drop(2).take(2).forEach { t ->
                                TemplateCard(
                                    template = t,
                                    onClick = {
                                        onCreateProject("${t.name} Edit", t.aspectRatio)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            defaultTemplates.drop(4).take(2).forEach { t ->
                                TemplateCard(
                                    template = t,
                                    onClick = {
                                        onCreateProject("${t.name} Edit", t.aspectRatio)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Project Options Dialog (Duplicate, Rename, Delete)
    if (selectedProjectForOptions != null) {
        val project = selectedProjectForOptions!!
        Dialog(onDismissRequest = { selectedProjectForOptions = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.5.dp, BorderBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = project.name,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${TimeUtils.formatDuration(project.durationMs)} · ${project.aspectRatio.label}",
                        color = SecondaryBlue,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    HorizontalDivider(color = DividerColor)
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
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Duplicate Project",
                            color = DeepNavy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Rename Project",
                            color = DeepNavy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Delete Project",
                            color = Color(0xFFE53935),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Dialogs from ViewModel State
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
fun TemplateCard(
    template: TemplateItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(BorderStroke(1.5.dp, BorderBlue), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = template.imgUrl,
            contentDescription = template.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color(0xFFE8F2FC))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = template.name,
                color = DeepNavy,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = template.duration,
                color = SecondaryBlue,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun HomeBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .border(BorderStroke(1.5.dp, BorderBlue))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            Triple("Home", Icons.Default.Home, 0),
            Triple("Edit", Icons.Default.ContentCut, 1),
            Triple("Template", Icons.Default.Collections, 2),
            Triple("Inbox", Icons.Default.Inbox, 3),
            Triple("Me", Icons.Default.Person, 4)
        )
        items.forEach { (label, icon, index) ->
            val isSelected = selectedIndex == index
            val color = if (isSelected) PrimaryBlue else InactiveNav
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = color
                )
            }
        }
    }
}
