package com.skyd.anivu.ui.screen.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.snapshotStateMapSaver
import com.skyd.anivu.model.bean.FeedBean
import com.skyd.anivu.model.bean.FeedBean.Companion.isDefaultGroup
import com.skyd.anivu.model.bean.FeedViewBean
import com.skyd.anivu.model.bean.GroupVo
import com.skyd.anivu.model.bean.GroupVo.Companion.isDefaultGroup
import com.skyd.anivu.model.preference.appearance.feed.FeedGroupExpandPreference
import com.skyd.anivu.ui.component.AniVuFloatingActionButton
import com.skyd.anivu.ui.component.AniVuIconButton
import com.skyd.anivu.ui.component.AniVuTopBar
import com.skyd.anivu.ui.component.AniVuTopBarStyle
import com.skyd.anivu.ui.component.ClipboardTextField
import com.skyd.anivu.ui.component.dialog.AniVuDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.component.lazyverticalgrid.AniVuLazyVerticalGrid
import com.skyd.anivu.ui.component.lazyverticalgrid.adapter.LazyGridAdapter
import com.skyd.anivu.ui.component.lazyverticalgrid.adapter.proxy.DefaultGroup1Proxy
import com.skyd.anivu.ui.component.lazyverticalgrid.adapter.proxy.Feed1Proxy
import com.skyd.anivu.ui.component.lazyverticalgrid.adapter.proxy.Group1Proxy
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.local.LocalFeedGroupExpand
import com.skyd.anivu.ui.local.LocalFeedListTonalElevation
import com.skyd.anivu.ui.local.LocalFeedTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalHideEmptyDefault
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.screen.article.ArticleScreen
import com.skyd.anivu.ui.screen.article.openArticleScreen
import com.skyd.anivu.ui.screen.feed.reorder.REORDER_GROUP_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.search.SearchDomain
import com.skyd.anivu.ui.screen.search.openSearchScreen
import kotlinx.coroutines.android.awaitFrame
import java.util.UUID

const val FEED_SCREEN_ROUTE = "feedScreen"

@Composable
fun FeedScreen() {
    val navigator = rememberListDetailPaneScaffoldNavigator<List<String>>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            horizontalPartitionSpacerSize = 0.dp,
        )
    )
    val navController = LocalNavController.current
    val windowSizeClass = LocalWindowSizeClass.current
    val density = LocalDensity.current

    var listPaneSelectedFeedUrls by remember { mutableStateOf<List<String>?>(null) }

    val onNavigatorBack: () -> Unit = {
        navigator.navigateBack()
        listPaneSelectedFeedUrls = navigator.currentDestination?.content
    }

    BackHandler(navigator.canNavigateBack()) {
        onNavigatorBack()
    }

    val windowWidth = with(density) { currentWindowSize().width.toDp() }
    val feedListWidth by remember(windowWidth) { mutableStateOf(windowWidth * 0.335f) }

    ListDetailPaneScaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(
            WindowInsetsSides.Right.run {
                if (windowSizeClass.isCompact) plus(WindowInsetsSides.Left) else this
            }
        )),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(feedListWidth)) {
                FeedList(
                    listPaneSelectedFeedUrls = listPaneSelectedFeedUrls,
                    onShowArticleList = { feedUrls ->
                        if (navigator.scaffoldDirective.maxHorizontalPartitions > 1) {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, feedUrls)
                        } else {
                            openArticleScreen(
                                navController = navController,
                                feedUrls = ArrayList(feedUrls),
                            )
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.content?.let {
                    listPaneSelectedFeedUrls = it
                    ArticleScreen(feedUrls = it, onBackClick = onNavigatorBack)
                }
            }
        },
    )
}

@Composable
private fun FeedList(
    listPaneSelectedFeedUrls: List<String>? = null,
    onShowArticleList: (List<String>) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val navController = LocalNavController.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var addDialogUrl by rememberSaveable { mutableStateOf("") }
    var openEditFeedDialog by rememberSaveable { mutableStateOf<FeedBean?>(null) }
    var openEditGroupDialog by rememberSaveable { mutableStateOf<GroupVo?>(value = null) }

    var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }

    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = FeedIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AniVuTopBar(
                style = AniVuTopBarStyle.Small,
                title = { Text(text = stringResource(id = R.string.feed_screen_name)) },
                actions = {
                    AniVuIconButton(
                        onClick = {
                            onShowArticleList(
                                (uiState.groupListState as? GroupListState.Success)
                                    ?.dataList
                                    ?.filterIsInstance<FeedViewBean>()
                                    ?.map { it.feed.url }
                                    .orEmpty()
                            )
                        },
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = stringResource(id = R.string.feed_screen_all_articles),
                    )
                    AniVuIconButton(
                        onClick = {
                            openSearchScreen(
                                navController = navController,
                                searchDomain = SearchDomain.Feed,
                            )
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(id = R.string.feed_screen_search_feed),
                    )
                    AniVuIconButton(
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(id = R.string.more),
                    )
                    MoreMenu(expanded = openMoreMenu, onDismissRequest = { openMoreMenu = false })
                },
                navigationIcon = {},
                windowInsets = WindowInsets.safeDrawing.only(
                    (WindowInsetsSides.Top + WindowInsetsSides.Right).run {
                        if (windowSizeClass.isCompact) plus(WindowInsetsSides.Left) else this
                    }
                ),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalFeedTopBarTonalElevation.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalFeedTopBarTonalElevation.current.dp + 4.dp
                    ),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AniVuFloatingActionButton(
                onClick = { openAddDialog = true },
                onSizeWithSinglePaddingChanged = { width, height ->
                    fabWidth = width
                    fabHeight = height
                },
                contentDescription = stringResource(R.string.add),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    LocalFeedListTonalElevation.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        when (val groupListState = uiState.groupListState) {
            is GroupListState.Failed, GroupListState.Init, GroupListState.Loading -> {}
            is GroupListState.Success -> {
                FeedList(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    result = groupListState.dataList,
                    contentPadding = innerPadding + PaddingValues(bottom = fabHeight + 16.dp),
                    selectedFeedUrls = listPaneSelectedFeedUrls,
                    onShowArticleList = { feedUrls -> onShowArticleList(feedUrls) },
                    onEditFeed = { feed -> openEditFeedDialog = feed },
                    onEditGroup = { group -> openEditGroupDialog = group },
                )
            }
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is FeedEvent.AddFeedResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is FeedEvent.InitFeetListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is FeedEvent.EditFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.RemoveFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.RefreshFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.CreateGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.MoveFeedsToGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.DeleteGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.EditGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.ReadAllResultEvent.Failed -> event.msg.showToast()

                is FeedEvent.EditFeedResultEvent.Success -> {
                    if (openEditFeedDialog != null) openEditFeedDialog = event.feed
                }

                is FeedEvent.EditGroupResultEvent.Success -> {
                    if (openEditGroupDialog != null) openEditGroupDialog = event.group
                }

                is FeedEvent.AddFeedResultEvent.Success -> openEditFeedDialog = event.feed

                is FeedEvent.ReadAllResultEvent.Success -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(
                        R.plurals.feed_screen_read_all_result,
                        event.count,
                        event.count,
                    ),
                )

                FeedEvent.RemoveFeedResultEvent.Success,
                is FeedEvent.RefreshFeedResultEvent.Success,
                FeedEvent.CreateGroupResultEvent.Success,
                FeedEvent.MoveFeedsToGroupResultEvent.Success,
                FeedEvent.DeleteGroupResultEvent.Success -> Unit
            }
        }

        if (openAddDialog) {
            AddFeedDialog(
                url = addDialogUrl,
                onUrlChange = { text -> addDialogUrl = text },
                onConfirm = { newUrl ->
                    if (newUrl.isNotBlank()) {
                        dispatch(FeedIntent.AddFeed(url = newUrl))
                    }
                    addDialogUrl = ""
                    openAddDialog = false
                },
                onDismissRequest = {
                    addDialogUrl = ""
                    openAddDialog = false
                }
            )
        }

        if (openEditFeedDialog != null) {
            val groups = remember(uiState.groupListState) {
                (uiState.groupListState as? GroupListState.Success)
                    ?.dataList?.filterIsInstance<GroupVo>().orEmpty()
            }
            EditFeedSheet(
                onDismissRequest = { openEditFeedDialog = null },
                feed = openEditFeedDialog!!,
                groups = groups,
                onReadAll = { dispatch(FeedIntent.ReadAllInFeed(it)) },
                onRefresh = { dispatch(FeedIntent.RefreshFeed(it)) },
                onDelete = { dispatch(FeedIntent.RemoveFeed(it)) },
                onUrlChange = {
                    dispatch(FeedIntent.EditFeedUrl(oldUrl = openEditFeedDialog!!.url, newUrl = it))
                },
                onNicknameChange = {
                    dispatch(
                        FeedIntent.EditFeedNickname(
                            url = openEditFeedDialog!!.url,
                            nickname = it
                        )
                    )
                },
                onCustomDescriptionChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomDescription(
                            url = openEditFeedDialog!!.url, customDescription = it,
                        )
                    )
                },
                onCustomIconChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomIcon(
                            url = openEditFeedDialog!!.url, customIcon = it,
                        )
                    )
                },
                onSortXmlArticlesOnUpdateChanged = {
                    dispatch(
                        FeedIntent.EditFeedSortXmlArticlesOnUpdate(
                            url = openEditFeedDialog!!.url, sort = it,
                        )
                    )
                },
                onGroupChange = {
                    dispatch(
                        FeedIntent.EditFeedGroup(
                            url = openEditFeedDialog!!.url,
                            groupId = it.groupId
                        )
                    )
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
            )
        }

        if (openEditGroupDialog != null) {
            val groups = remember(uiState.groupListState) {
                (uiState.groupListState as? GroupListState.Success)
                    ?.dataList?.filterIsInstance<GroupVo>().orEmpty()
            }
            EditGroupSheet(
                onDismissRequest = { openEditGroupDialog = null },
                group = openEditGroupDialog!!,
                groups = groups,
                onReadAll = { dispatch(FeedIntent.ReadAllInGroup(it)) },
                onRefresh = { dispatch(FeedIntent.RefreshGroupFeed(it)) },
                onDelete = { dispatch(FeedIntent.DeleteGroup(it)) },
                onNameChange = {
                    dispatch(
                        FeedIntent.RenameGroup(groupId = openEditGroupDialog!!.groupId, name = it)
                    )
                },
                onMoveTo = {
                    dispatch(
                        FeedIntent.MoveFeedsToGroup(
                            fromGroupId = openEditGroupDialog!!.groupId,
                            toGroupId = it.groupId,
                        )
                    )
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
            )
        }

        WaitingDialog(visible = uiState.loadingDialog)

        CreateGroupDialog(
            visible = openCreateGroupDialog,
            value = createGroupDialogGroup,
            onValueChange = { text -> createGroupDialogGroup = text },
            onCreateGroup = {
                dispatch(FeedIntent.CreateGroup(it))
                openCreateGroupDialog = false
            },
            onDismissRequest = {
                openCreateGroupDialog = false
            }
        )
    }
}

@Composable
private fun AddFeedDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: (url: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AniVuDialog(
        visible = true,
        icon = { Icon(imageVector = Icons.Outlined.RssFeed, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.add)) },
        onDismissRequest = onDismissRequest,
        selectable = false,
        text = {
            Column {
                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                ClipboardTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    value = url,
                    onValueChange = onUrlChange,
                    autoRequestFocus = false,
                    label = stringResource(id = R.string.feed_screen_rss_url),
                    focusManager = focusManager,
                    imeAction = ImeAction.Next,
                    keyboardAction = { _, _ ->
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                )

                LaunchedEffect(focusRequester) {
                    focusRequester.requestFocus()
                    awaitFrame()
                    keyboard?.show()
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onConfirm(url) }
            ) {
                Text(
                    text = stringResource(R.string.ok),
                    color = if (url.isNotBlank()) Color.Unspecified
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CreateGroupDialog(
    visible: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onCreateGroup: (GroupVo) -> Unit,
    onDismissRequest: () -> Unit,
) {
    TextFieldDialog(
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Workspaces, contentDescription = null) },
        titleText = stringResource(id = R.string.feed_screen_add_group),
        placeholder = stringResource(id = R.string.feed_group),
        value = value,
        onValueChange = onValueChange,
        onConfirm = { text ->
            onCreateGroup(
                GroupVo(
                    groupId = UUID.randomUUID().toString(),
                    name = text,
                    isExpanded = true,
                )
            )
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun FeedList(
    modifier: Modifier = Modifier,
    result: List<Any>,
    contentPadding: PaddingValues = PaddingValues(),
    selectedFeedUrls: List<String>? = null,
    onShowArticleList: (List<String>) -> Unit,
    onEditFeed: (FeedBean) -> Unit,
    onEditGroup: (GroupVo) -> Unit,
) {
    val context = LocalContext.current
    val hideEmptyDefault = LocalHideEmptyDefault.current
    val feedGroupExpand = LocalFeedGroupExpand.current
    val groups by remember { derivedStateOf { result.filterIsInstance<GroupVo>() } }
    val feedVisible = rememberSaveable(saver = snapshotStateMapSaver()) {
        mutableStateMapOf(
            GroupVo.DEFAULT_GROUP_ID to feedGroupExpand,
            *(groups
                .map { it.groupId to feedGroupExpand }
                .toTypedArray())
        )
    }
    // Update feedVisible when groups change
    LaunchedEffect(groups) {
        feedVisible.forEach { (t, u) ->
            if (groups.find { it.groupId == t } == null) {
                feedVisible.remove(t)
            } else {
                feedVisible[t] = u
            }
        }
        val defaultExpand = context.dataStore.getOrDefault(FeedGroupExpandPreference)
        groups.forEach {
            feedVisible[it.groupId] = feedVisible[it.groupId] ?: defaultExpand
        }
    }

    val adapter = remember(result, hideEmptyDefault, selectedFeedUrls) {
        val group1Proxy = Group1Proxy(
            isExpand = { feedVisible[it.groupId] ?: false },
            onExpandChange = { data, expand -> feedVisible[data.groupId] = expand },
            isEmpty = { it == result.lastIndex || result[it + 1] is GroupVo },
            onShowAllArticles = { group ->
                val feedUrls = result
                    .filterIsInstance<FeedViewBean>()
                    .filter { it.feed.groupId == group.groupId || group.isDefaultGroup() && it.feed.isDefaultGroup() }
                    .map { it.feed.url }
                onShowArticleList(feedUrls)
            },
            onEdit = onEditGroup,
        )
        LazyGridAdapter(
            mutableListOf(
                DefaultGroup1Proxy(
                    group1Proxy = group1Proxy,
                    hide = { hideEmptyDefault && result.getOrNull(it + 1) !is FeedViewBean },
                ),
                group1Proxy,
                Feed1Proxy(
                    visible = { feedVisible[it] ?: false },
                    selected = { selectedFeedUrls != null && it.url in selectedFeedUrls },
                    inGroup = { true },
                    onClick = { onShowArticleList(listOf(it.url)) },
                    isEnded = { it == result.lastIndex || result[it + 1] is GroupVo },
                    onEdit = onEditFeed
                )
            )
        )
    }
    AniVuLazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(1),
        dataList = result,
        adapter = adapter,
        contentPadding = contentPadding + PaddingValues(horizontal = 16.dp),
        key = { _, item ->
            when (item) {
                is GroupVo.DefaultGroup -> item.groupId
                is GroupVo -> item.groupId
                is FeedViewBean -> item.feed.url
                else -> item
            }
        },
    )
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navController = LocalNavController.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.reorder_group_screen_name)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                navController.navigate(REORDER_GROUP_SCREEN_ROUTE)
            },
        )
    }
}