package com.chronicle.expense.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.DateRange
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.TxFilter
import com.chronicle.expense.data.CATEGORY_AUTO_LOG
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.toLocalDate
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.Chip
import com.chronicle.expense.ui.components.DayPickerDialog
import com.chronicle.expense.ui.components.EmptyState
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.FieldLabel
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.IconBtn
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.QuietButton
import com.chronicle.expense.ui.components.TopBar
import com.chronicle.expense.ui.components.TxRow
import com.chronicle.expense.ui.components.datePickerColors
import com.chronicle.expense.ui.components.formatDay
import com.chronicle.expense.ui.components.sanitizeAmount
import com.chronicle.expense.ui.components.utcMillis
import com.chronicle.expense.ui.components.utcMillisToDate
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TransactionsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.transactionList.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val names = remember(accounts) { accounts.associate { it.id to it.name } }
    val accountName: (String) -> String = { names[it] ?: "Deleted account" }
    var showFilters by remember { mutableStateOf(false) }

    val groups = remember(state.items) { state.items.groupBy { it.timestamp.toLocalDate() }.toList() }
    val spent = state.items.filter { it.kind == TxKind.EXPENSE }.sumOf { it.amount }
    val earned = state.items.filter { it.kind == TxKind.INCOME }.sumOf { it.amount }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        TopBar("Transactions", onBack) {
            Box {
                IconBtn(Icons.Outlined.Tune, "Filters", { showFilters = true }, tint = if (filter.count > 0) C.primary else C.text)
                if (filter.count > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp).size(16.dp).clip(CircleShape).background(C.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${filter.count}", style = Type.overline.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), color = C.onPrimary)
                    }
                }
            }
        }

        Box(Modifier.padding(horizontal = 20.dp)) {
            // Sits on the page background, so it uses the card surface instead of the inset fill.
            CardBox(Modifier.fillMaxWidth()) {
                SearchField(filter.query) { vm.filter.value = filter.copy(query = it) }
            }
        }

        ActiveFilters(filter, accountName) { vm.filter.value = it }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = navInset + 24.dp),
        ) {
            if (state.items.isNotEmpty()) {
                item("summary") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp)) {
                        Text(
                            "${state.items.size} ${if (state.items.size == 1) "transaction" else "transactions"}",
                            style = Type.caption, color = C.textSecondary, modifier = Modifier.weight(1f),
                        )
                        Text("Out ${Money.format(spent)}", style = Type.caption.copy(fontFeatureSettings = "tnum"), color = C.textSecondary)
                        Text("  ·  ", style = Type.caption, color = C.textTertiary)
                        Text("In ${Money.format(earned)}", style = Type.caption.copy(fontFeatureSettings = "tnum"), color = C.income)
                    }
                }
            }
            groups.forEach { (date, list) ->
                item(date.toString()) {
                    val net = list.sumOf {
                        when (it.kind) {
                            TxKind.EXPENSE -> -it.amount
                            TxKind.INCOME -> it.amount
                            TxKind.TRANSFER, TxKind.DEBT_OUT, TxKind.DEBT_IN -> 0L
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp)) {
                        Text(formatDay(list.first().timestamp), style = Type.label, color = C.textSecondary, modifier = Modifier.weight(1f))
                        if (net != 0L) {
                            Text(
                                (if (net > 0) "+" else "") + Money.format(net),
                                style = Type.caption.copy(fontFeatureSettings = "tnum"), color = C.textTertiary,
                            )
                        }
                    }
                    CardBox(Modifier.fillMaxWidth()) {
                        list.forEachIndexed { i, tx ->
                            if (i > 0) Hairline(inset = 68.dp)
                            TxRow(tx, accountName, onClick = { vm.sheet = Sheet.EditTx(tx) })
                        }
                    }
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item("empty") {
                    EmptyState(
                        Icons.Outlined.SearchOff, "No transactions found",
                        if (filter.count > 0 || filter.query.isNotBlank()) "Try removing a filter or widening the date range."
                        else "Everything you log will be listed here.",
                        Modifier.padding(top = 48.dp),
                    )
                }
            }
            if (state.hasMore) {
                item("more") {
                    QuietButton("Load older transactions", vm::loadMore, Modifier.padding(top = 20.dp))
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(vm, filter, onApply = { vm.resetPaging(); vm.filter.value = it; showFilters = false }) { showFilters = false }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search category, note or amount", style = Type.body, color = C.textTertiary)
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { onChange(it.take(60)) },
                singleLine = true,
                textStyle = Type.body.copy(color = C.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close, "Clear search", tint = C.textSecondary,
                modifier = Modifier.clip(CircleShape).clickable { onChange("") }.padding(4.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun ActiveFilters(filter: TxFilter, accountName: (String) -> String, onChange: (TxFilter) -> Unit) {
    if (filter.count == 0) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filter.range != DateRange.AllTime) {
            Chip(filter.range.label, true, { onChange(filter.copy(range = DateRange.AllTime)) }, leading = Icons.Outlined.Close)
        }
        filter.kinds.forEach { k ->
            Chip(k.label, true, { onChange(filter.copy(kinds = filter.kinds - k)) }, leading = Icons.Outlined.Close)
        }
        filter.accountIds.forEach { id ->
            Chip(accountName(id), true, { onChange(filter.copy(accountIds = filter.accountIds - id)) }, leading = Icons.Outlined.Close)
        }
        filter.categories.forEach { c ->
            Chip(c, true, { onChange(filter.copy(categories = filter.categories - c)) }, leading = Icons.Outlined.Close)
        }
        if (filter.min != null || filter.max != null) {
            val label = when {
                filter.min != null && filter.max != null -> "${Money.format(filter.min)} – ${Money.format(filter.max)}"
                filter.min != null -> "From ${Money.format(filter.min)}"
                else -> "Up to ${Money.format(filter.max!!)}"
            }
            Chip(label, true, { onChange(filter.copy(min = null, max = null)) }, leading = Icons.Outlined.Close)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(vm: MainViewModel, initial: TxFilter, onApply: (TxFilter) -> Unit, onDismiss: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(initial) }
    var min by remember { mutableStateOf(initial.min?.let(Money::toInput) ?: "") }
    var max by remember { mutableStateOf(initial.max?.let(Money::toInput) ?: "") }
    var pickDay by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }
    var pickRange by remember { mutableStateOf(false) }

    val gap = Arrangement.spacedBy(8.dp)

    AppSheet(onDismiss, title = "Filters") {
        FieldLabel("Type")
        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            Chip("All", draft.kinds.isEmpty(), { draft = draft.copy(kinds = emptySet()) })
            TxKind.entries.forEach { k ->
                Chip(k.label, k in draft.kinds, {
                    draft = draft.copy(kinds = if (k in draft.kinds) draft.kinds - k else draft.kinds + k)
                })
            }
        }

        Spacer(Modifier.height(20.dp))
        FieldLabel("Date")
        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            listOf(DateRange.AllTime, DateRange.Today, DateRange.ThisWeek, DateRange.ThisMonth).forEach { r ->
                Chip(r.label, draft.range == r, { draft = draft.copy(range = r) })
            }
            val range = draft.range
            Chip(if (range is DateRange.Day) range.label else "Specific date", range is DateRange.Day, { pickDay = true })
            Chip(if (range is DateRange.Month) range.label else "Month", range is DateRange.Month, { pickMonth = true })
            Chip(if (range is DateRange.Custom) range.label else "Date range", range is DateRange.Custom, { pickRange = true })
        }

        if (accounts.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            FieldLabel("Accounts")
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                accounts.forEach { a ->
                    Chip(a.name, a.id in draft.accountIds, {
                        draft = draft.copy(accountIds = if (a.id in draft.accountIds) draft.accountIds - a.id else draft.accountIds + a.id)
                    })
                }
            }
        }

        val names = (categories.map { it.name } + CATEGORY_AUTO_LOG).distinct().sortedBy { it.lowercase() }
        if (names.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            FieldLabel("Categories and sources")
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                names.forEach { n ->
                    Chip(n, n in draft.categories, {
                        draft = draft.copy(categories = if (n in draft.categories) draft.categories - n else draft.categories + n)
                    })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        FieldLabel("Amount")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(
                min, { min = sanitizeAmount(it) }, "Min", Modifier.weight(1f),
                keyboardType = KeyboardType.Decimal, capitalization = KeyboardCapitalization.None,
                leading = { Text(Money.SYMBOL, style = Type.body, color = C.textSecondary) },
            )
            Field(
                max, { max = sanitizeAmount(it) }, "Max", Modifier.weight(1f),
                keyboardType = KeyboardType.Decimal, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done,
                leading = { Text(Money.SYMBOL, style = Type.body, color = C.textSecondary) },
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietButton("Reset", { onApply(TxFilter(query = initial.query)) }, Modifier.weight(1f))
            PrimaryButton("Apply", {
                onApply(draft.copy(min = Money.parse(min)?.takeIf { it > 0 }, max = Money.parse(max)?.takeIf { it > 0 }))
            }, Modifier.weight(1.6f))
        }
    }

    if (pickDay) {
        DayPickerDialog(
            initial = (draft.range as? DateRange.Day)?.date ?: LocalDate.now(),
            onPick = { draft = draft.copy(range = DateRange.Day(it)); pickDay = false },
            onDismiss = { pickDay = false },
        )
    }
    if (pickMonth) {
        MonthPickerSheet(
            initial = (draft.range as? DateRange.Month)?.month ?: YearMonth.now(),
            onPick = { draft = draft.copy(range = DateRange.Month(it)); pickMonth = false },
            onDismiss = { pickMonth = false },
        )
    }
    if (pickRange) {
        val current = draft.range as? DateRange.Custom
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = current?.from?.utcMillis(),
            initialSelectedEndDateMillis = current?.to?.utcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { pickRange = false },
            colors = datePickerColors(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 0.dp,
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val from = utcMillisToDate(state.selectedStartDateMillis!!)
                        val to = state.selectedEndDateMillis?.let(::utcMillisToDate) ?: from
                        draft = draft.copy(range = DateRange.Custom(from, to))
                        pickRange = false
                    },
                ) { Text("Done", style = Type.bodyMedium, color = C.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pickRange = false }) { Text("Cancel", style = Type.bodyMedium, color = C.textSecondary) }
            },
        ) {
            DateRangePicker(
                state, colors = datePickerColors(), showModeToggle = false,
                title = { Text("Select a date range", style = Type.label, color = C.textSecondary, modifier = Modifier.padding(start = 24.dp, top = 20.dp)) },
                modifier = Modifier.height(480.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthPickerSheet(initial: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableIntStateOf(initial.year) }
    val now = YearMonth.now()
    AppSheet(onDismiss, title = "Pick a month") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous year", { year-- })
            Text(
                "$year", style = Type.heading.copy(fontFeatureSettings = "tnum"), color = C.text,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconBtn(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next year", { if (year < now.year) year++ },
                tint = if (year < now.year) C.text else C.textTertiary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Month.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m ->
                    val ym = YearMonth.of(year, m)
                    val future = ym.isAfter(now)
                    val selected = ym == initial
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) C.primary else C.surfaceAlt)
                            .clickable(enabled = !future) { onPick(ym) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            m.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = Type.label,
                            color = when {
                                selected -> C.onPrimary
                                future -> C.textTertiary
                                else -> C.text
                            },
                        )
                    }
                }
            }
        }
    }
}
