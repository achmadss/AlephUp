package dev.achmad.alephup.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.achmad.alephup.ui.components.AppBar
import dev.achmad.alephup.ui.components.ScrollbarLazyColumn
import dev.achmad.alephup.ui.settings.widget.PreferenceGroupHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceScreen(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
    topBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    ),
) {
    val items = itemsProvider()
    Scaffold(
        topBar = {
            AppBar(
                title = title,
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = topBarScrollBehavior,
            )
        },
        content = { contentPadding ->
            val lazyListState = rememberLazyListState()
            ScrollbarLazyColumn(
                modifier = modifier,
                state = lazyListState,
                contentPadding = contentPadding,
            ) {
                items.fastForEachIndexed { i, preference ->
                    when (preference) {
                        // Create Preference Group
                        is Preference.PreferenceGroup -> {
                            if (!preference.visible) return@fastForEachIndexed

                            item {
                                Column {
                                    PreferenceGroupHeader(title = preference.title)
                                }
                            }
                            items(preference.preferenceItems) { item ->
                                PreferenceItem(
                                    item = item,
                                    highlightKey = null,
                                )
                            }
                            item {
                                if (i < items.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        // Create Preference Item
                        is Preference.PreferenceItem<*> -> item {
                            PreferenceItem(
                                item = preference,
                                highlightKey = null,
                            )
                        }
                    }
                }
            }
        },
    )
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(null) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
