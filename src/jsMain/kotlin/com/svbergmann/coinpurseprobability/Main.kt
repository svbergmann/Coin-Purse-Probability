package com.svbergmann.coinpurseprobability

import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.unsafeCast
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node

private val purse = CoinPurse()
private const val STORAGE_KEY = "coin-purse-probability.state"
private const val THEME_STORAGE_KEY = "coin-purse-probability.theme"

private var selectedCoin: GameCoin = GameCoin.entries.first()
private var currentLang: Lang = Lang.DE
private var currentTheme: Theme = Theme.DARK
private var lastActionProvider: () -> String = { tr().ready }
private var dropdownOptions: List<HTMLButtonElement> = emptyList()
private var dropdownHighlightIndex: Int = 0

private enum class Lang { DE, EN }
private enum class Theme { DARK, LIGHT }

private data class Strings(
    val coin: String,
    val quantity: String,
    val add: String,
    val remove: String,
    val draw: String,
    val resetDrawn: String,
    val resetStarter: String,
    val probTitle: String,
    val drawnTitle: String,
    val thCoin: String,
    val thCount: String,
    val thFraction: String,
    val thChance: String,
    val thAction: String,
    val coinsInPurse: String,
    val none: String,
    val ready: String,
    val purseEmpty: String,
    val resetDrawnDone: String,
    val resetStarterDone: String,
    val addedPrefix: String,
    val removedPrefix: String,
    val removeFailedPrefix: String,
    val drawnPrefix: String,
    val drawThis: String,
    val chance: String,
    val unavailable: String,
    val darkMode: String,
    val lightMode: String,
    val langToggle: String,
    val langToggleTitle: String,
)

private val de = Strings(
    coin = "Münze",
    quantity = "Anzahl",
    add = "Hinzufügen",
    remove = "Entfernen",
    draw = "Ziehen (Zufällig)",
    resetDrawn = "Gezogene zurück",
    resetStarter = "Startbeutel",
    probTitle = "Verbleibende Zieh-Wahrscheinlichkeit",
    drawnTitle = "Gezogene Münzen",
    thCoin = "Münze",
    thCount = "Anzahl",
    thFraction = "Bruch",
    thChance = "Chance",
    thAction = "Ziehen",
    coinsInPurse = "Münzen im Beutel",
    none = "Keine",
    ready = "Bereit",
    purseEmpty = "Beutel ist leer",
    resetDrawnDone = "Gezogene Münzen zurückgesetzt",
    resetStarterDone = "Auf Startbeutel zurückgesetzt",
    addedPrefix = "Hinzugefügt",
    removedPrefix = "Entfernt",
    removeFailedPrefix = "Konnte nicht entfernen",
    drawnPrefix = "Gezogen",
    drawThis = "Ziehen",
    chance = "Chance",
    unavailable = "Nicht verfügbar",
    darkMode = "Dunkelmodus",
    lightMode = "Hellmodus",
    langToggle = "🇩🇪 DE",
    langToggleTitle = "Zu Englisch wechseln",
)

private val en = Strings(
    coin = "Coin",
    quantity = "Quantity",
    add = "Add",
    remove = "Remove",
    draw = "Draw (Random)",
    resetDrawn = "Reset Drawn",
    resetStarter = "Reset Starter",
    probTitle = "Remaining Draw Probability",
    drawnTitle = "Drawn Coins",
    thCoin = "Coin",
    thCount = "Count",
    thFraction = "Fraction",
    thChance = "Chance",
    thAction = "Draw",
    coinsInPurse = "Coins in purse",
    none = "None",
    ready = "Ready",
    purseEmpty = "Purse is empty",
    resetDrawnDone = "Reset drawn coins",
    resetStarterDone = "Reset to starter purse",
    addedPrefix = "Added",
    removedPrefix = "Removed",
    removeFailedPrefix = "Could not remove",
    drawnPrefix = "Drawn",
    drawThis = "Draw",
    chance = "Chance",
    unavailable = "Unavailable",
    darkMode = "Dark mode",
    lightMode = "Light mode",
    langToggle = "🇬🇧 EN",
    langToggleTitle = "Switch to German",
)

private fun tr(): Strings = if (currentLang == Lang.DE) de else en

fun main() {
    val languageToggle = elementById<HTMLButtonElement>("language-toggle")
    val coinDropdown = elementById<HTMLDivElement>("coin-dropdown")
    val coinToggle = elementById<HTMLButtonElement>("coin-dropdown-toggle")
    val coinMenu = elementById<HTMLDivElement>("coin-dropdown-menu")
    val qtyInput = elementById<HTMLInputElement>("quantity-input")
    val addButton = elementById<HTMLButtonElement>("add-button")
    val removeButton = elementById<HTMLButtonElement>("remove-button")
    val drawButton = elementById<HTMLButtonElement>("draw-button")
    val resetDrawnButton = elementById<HTMLButtonElement>("reset-drawn-button")
    val resetStarterButton = elementById<HTMLButtonElement>("reset-starter-button")
    val darkModeToggle = elementById<HTMLButtonElement>("darkmode-toggle")

    loadPersistedState()
    loadTheme()
    applyTheme(darkModeToggle)

    languageToggle.onclick = {
        currentLang = if (currentLang == Lang.DE) Lang.EN else Lang.DE
        applyTexts()
        renderCoinDropdown(coinMenu, coinToggle, coinDropdown)
        render()
        null
    }
    darkModeToggle.onclick = {
        currentTheme = if (currentTheme == Theme.DARK) Theme.LIGHT else Theme.DARK
        saveTheme()
        applyTheme(darkModeToggle)
        null
    }

    coinToggle.onclick = {
        if (coinDropdown.classList.contains("open")) {
            closeDropdown(coinDropdown)
        } else {
            openDropdown(coinDropdown, coinToggle)
        }
        null
    }
    coinToggle.onkeydown = { event ->
        when (event.asDynamic().key as String) {
            "ArrowDown", "ArrowUp", "Enter", " " -> {
                event.preventDefault()
                if (!coinDropdown.classList.contains("open")) {
                    openDropdown(coinDropdown, coinToggle)
                } else {
                    focusDropdownOption(
                        if ((event.asDynamic().key as String) == "ArrowUp") dropdownHighlightIndex - 1 else dropdownHighlightIndex + 1
                    )
                }
            }
            "Escape" -> {
                event.preventDefault()
                closeDropdown(coinDropdown)
            }
        }
        null
    }

    document.addEventListener("click", { event ->
        val target = event.asDynamic().target as? Node ?: return@addEventListener
        if (!coinDropdown.contains(target)) {
            closeDropdown(coinDropdown)
        }
    })

    renderCoinDropdown(coinMenu, coinToggle, coinDropdown)

    addButton.onclick = {
        val quantity = readQuantity(qtyInput)
        purse.addCoin(selectedCoin, quantity)
        saveState()
        lastActionProvider = { "${tr().addedPrefix} $quantity x ${coinDisplayName(selectedCoin)}" }
        render()
        null
    }

    removeButton.onclick = {
        val quantity = readQuantity(qtyInput)
        val success = purse.removeCoin(selectedCoin, quantity)
        lastActionProvider = {
            if (success) {
                "${tr().removedPrefix} $quantity x ${coinDisplayName(selectedCoin)}"
            } else {
                "${tr().removeFailedPrefix} $quantity x ${coinDisplayName(selectedCoin)}"
            }
        }
        saveState()
        render()
        null
    }

    drawButton.onclick = {
        if (purse.totalCoinCount() == 0) {
            lastActionProvider = { tr().purseEmpty }
        } else {
            val drawn = purse.drawRandom()
            saveState()
            lastActionProvider = { "${tr().drawnPrefix}: ${coinDisplayName(drawn.coin)} (${tr().chance}: ${formatProbability(drawn)})" }
        }
        render()
        null
    }

    resetDrawnButton.onclick = {
        purse.resetDrawn()
        saveState()
        lastActionProvider = { tr().resetDrawnDone }
        render()
        null
    }

    resetStarterButton.onclick = {
        purse.resetToStarterPurse()
        saveState()
        lastActionProvider = { tr().resetStarterDone }
        render()
        null
    }

    applyTexts()
    lastActionProvider = { tr().ready }
    render()
}

private fun renderCoinDropdown(menu: HTMLDivElement, toggle: HTMLButtonElement, dropdown: HTMLDivElement) {
    menu.innerHTML = GameCoin.entries.joinToString(separator = "") { coin ->
        "<button type=\"button\" class=\"coin-option\" data-coin=\"${coin.name}\">${coinBadge(coin)}</button>"
    }

    val options = menu.querySelectorAll(".coin-option")
    val mutableOptions = mutableListOf<HTMLButtonElement>()
    for (i in 0 until options.length) {
        val option = options.item(i).unsafeCast<HTMLButtonElement>()
        mutableOptions += option
        option.onclick = {
            val name = option.getAttribute("data-coin")
            if (name != null) {
                selectedCoin = GameCoin.valueOf(name)
                toggle.innerHTML = coinBadge(selectedCoin)
                closeDropdown(dropdown)
                toggle.focus()
            }
            null
        }
        option.onkeydown = { event ->
            when (event.asDynamic().key as String) {
                "ArrowDown" -> {
                    event.preventDefault()
                    focusDropdownOption(dropdownHighlightIndex + 1)
                }
                "ArrowUp" -> {
                    event.preventDefault()
                    focusDropdownOption(dropdownHighlightIndex - 1)
                }
                "Escape" -> {
                    event.preventDefault()
                    closeDropdown(dropdown)
                    toggle.focus()
                }
                "Enter", " " -> {
                    event.preventDefault()
                    option.click()
                }
            }
            null
        }
    }
    dropdownOptions = mutableOptions

    toggle.innerHTML = coinBadge(selectedCoin)
    toggle.setAttribute("aria-expanded", "false")
    applyCoinDropdownWidth(menu, toggle, dropdown)
}

private fun applyCoinDropdownWidth(menu: HTMLDivElement, toggle: HTMLButtonElement, dropdown: HTMLDivElement) {
    val oldDisplay = menu.style.display
    val oldVisibility = menu.style.visibility

    // Temporarily reveal menu to measure widest rendered option.
    menu.style.display = "block"
    menu.style.visibility = "hidden"

    var maxWidth = 0.0
    for (option in dropdownOptions) {
        maxWidth = kotlin.math.max(maxWidth, option.scrollWidth.toDouble())
    }

    menu.style.display = oldDisplay
    menu.style.visibility = oldVisibility

    val targetWidth = (maxWidth + 22).toInt().coerceAtLeast(120)
    val widthPx = "${targetWidth}px"
    dropdown.style.width = widthPx
    toggle.style.width = widthPx
    menu.style.minWidth = widthPx
}

private fun applyTexts() {
    val s = tr()
    elementById<org.w3c.dom.HTMLElement>("coin-label").textContent = s.coin
    elementById<org.w3c.dom.HTMLElement>("quantity-label").textContent = s.quantity
    elementById<org.w3c.dom.HTMLElement>("add-button").textContent = s.add
    elementById<org.w3c.dom.HTMLElement>("remove-button").textContent = s.remove
    elementById<org.w3c.dom.HTMLElement>("draw-button").textContent = s.draw
    elementById<org.w3c.dom.HTMLElement>("reset-drawn-button").textContent = s.resetDrawn
    elementById<org.w3c.dom.HTMLElement>("reset-starter-button").textContent = s.resetStarter
    elementById<org.w3c.dom.HTMLElement>("prob-title").textContent = s.probTitle
    elementById<org.w3c.dom.HTMLElement>("drawn-title").textContent = s.drawnTitle
    elementById<org.w3c.dom.HTMLElement>("th-coin").textContent = s.thCoin
    elementById<org.w3c.dom.HTMLElement>("th-count").textContent = s.thCount
    elementById<org.w3c.dom.HTMLElement>("th-fraction").textContent = s.thFraction
    elementById<org.w3c.dom.HTMLElement>("th-chance").textContent = s.thChance
    elementById<org.w3c.dom.HTMLElement>("th-action").textContent = s.thAction
    updateLanguageToggle(elementById("language-toggle"))
    updateThemeToggle(elementById("darkmode-toggle"))
}

private fun render() {
    val action = elementById<org.w3c.dom.HTMLElement>("last-action")
    val totals = elementById<org.w3c.dom.HTMLElement>("totals")
    val probs = elementById<org.w3c.dom.HTMLElement>("probabilities")
    val drawnList = elementById<org.w3c.dom.HTMLElement>("drawn-coins")

    action.textContent = lastActionProvider()
    totals.textContent = "${tr().coinsInPurse}: ${purse.totalCoinCount()}"

    probs.innerHTML = purse.probabilities().joinToString(separator = "") { row ->
        val pct = (row.probability * 100).toFixed(2)
        """
        <tr>
          <td>${coinBadge(row.coin)}</td>
          <td>${row.count}</td>
          <td>${row.numerator}/${row.denominator}</td>
          <td>${pct}%</td>
          <td><button type="button" class="draw-row-button" data-coin="${row.coin.name}">${tr().drawThis}</button></td>
        </tr>
        """.trimIndent()
    }
    attachRowDrawHandlers()

    val drawn = purse.drawnCoins()
    drawnList.innerHTML = if (drawn.isEmpty()) {
        "<li>${tr().none}</li>"
    } else {
        drawn.mapIndexed { index, item ->
            "<li class=\"drawn-row\"><span class=\"drawn-index\">${index + 1}.</span><span class=\"drawn-item\"><span class=\"drawn-coin\">${coinBadge(item.coin)}</span><span class=\"chance-meta\">(${tr().chance}: ${formatProbability(item)})</span></span></li>"
        }
            .joinToString(separator = "")
    }
}

private fun attachRowDrawHandlers() {
    val buttons = document.querySelectorAll(".draw-row-button")
    for (i in 0 until buttons.length) {
        val button = buttons.item(i).unsafeCast<HTMLButtonElement>()
        button.onclick = {
            val coinName = button.getAttribute("data-coin")
            if (coinName != null) {
                val coin = GameCoin.valueOf(coinName)
                val drawn = purse.drawSpecific(coin)
                if (drawn == null) {
                    lastActionProvider = { "${tr().unavailable}: ${coinDisplayName(coin)}" }
                } else {
                    saveState()
                    lastActionProvider = { "${tr().drawnPrefix}: ${coinDisplayName(drawn.coin)} (${tr().chance}: ${formatProbability(drawn)})" }
                }
                render()
            }
            null
        }
    }
}

private inline fun <reified T> elementById(id: String): T {
    val element = document.getElementById(id)
        ?: error("Element '$id' not found")
    return element.unsafeCast<T>()
}

private fun readQuantity(input: HTMLInputElement): Int {
    val parsed = input.value.toIntOrNull() ?: 1
    return parsed.coerceAtLeast(1)
}

private fun Double.toFixed(digits: Int): String {
    return asDynamic().toFixed(digits) as String
}

private fun saveState() {
    window.localStorage.setItem(STORAGE_KEY, purse.exportState())
}

private fun loadPersistedState() {
    val raw = window.localStorage.getItem(STORAGE_KEY) ?: return
    val loaded = purse.importState(raw)
    if (!loaded) {
        window.localStorage.removeItem(STORAGE_KEY)
    }
}

private fun saveTheme() {
    window.localStorage.setItem(THEME_STORAGE_KEY, currentTheme.name)
}

private fun loadTheme() {
    val raw = window.localStorage.getItem(THEME_STORAGE_KEY) ?: return
    currentTheme = if (raw == Theme.LIGHT.name) Theme.LIGHT else Theme.DARK
}

private fun applyTheme(toggleButton: HTMLButtonElement) {
    val body = document.body ?: return
    body.classList.toggle("dark", currentTheme == Theme.DARK)
    updateThemeToggle(toggleButton)
}

private fun updateThemeToggle(toggleButton: HTMLButtonElement) {
    val darkModeOn = currentTheme == Theme.DARK
    toggleButton.textContent = if (darkModeOn) "☀️" else "🌙"
    toggleButton.title = if (darkModeOn) tr().lightMode else tr().darkMode
    toggleButton.setAttribute("aria-label", toggleButton.title)
}

private fun updateLanguageToggle(toggleButton: HTMLButtonElement) {
    val s = tr()
    toggleButton.textContent = s.langToggle
    toggleButton.title = s.langToggleTitle
    toggleButton.setAttribute("aria-label", s.langToggleTitle)
}

private fun openDropdown(dropdown: HTMLDivElement, toggle: HTMLButtonElement) {
    dropdown.classList.add("open")
    dropdownHighlightIndex = GameCoin.entries.indexOf(selectedCoin).coerceAtLeast(0)
    focusDropdownOption(dropdownHighlightIndex)
    toggle.setAttribute("aria-expanded", "true")
}

private fun closeDropdown(dropdown: HTMLDivElement) {
    dropdown.classList.remove("open")
    elementById<HTMLButtonElement>("coin-dropdown-toggle").setAttribute("aria-expanded", "false")
}

private fun focusDropdownOption(index: Int) {
    if (dropdownOptions.isEmpty()) return
    val normalized = when {
        index < 0 -> dropdownOptions.size - 1
        index >= dropdownOptions.size -> 0
        else -> index
    }
    dropdownHighlightIndex = normalized
    dropdownOptions.forEachIndexed { i, option ->
        if (i == normalized) option.classList.add("is-active") else option.classList.remove("is-active")
    }
    dropdownOptions[normalized].focus()
}

private fun formatProbability(drawn: DrawnCoin): String {
    if (drawn.denominator <= 0) return "-"
    return "${(drawn.probabilityAtDraw * 100).toFixed(2)}%"
}

private fun coinBadge(coin: GameCoin): String {
    val fill = coinFillColor(coin)
    val stroke = coinStrokeColor(coin)
    val text = coinTextColor(coin)
    val value = coin.value
    return """
    <span class="coin-badge">
      <svg class="coin-svg" viewBox="0 0 28 28" role="img" aria-label="${coinDisplayName(coin)}">
        <circle cx="14" cy="14" r="11.5" fill="$fill" stroke="$stroke" stroke-width="2" />
        <text x="14" y="18" text-anchor="middle" font-size="11" font-weight="700" fill="$text">$value</text>
      </svg>
      <span class="coin-label">${localizedColor(coin.color)}</span>
    </span>
    """.trimIndent()
}

private fun coinDisplayName(coin: GameCoin): String = "${localizedColor(coin.color)} ${coin.value}"

private fun localizedColor(color: String): String {
    return if (currentLang == Lang.DE) {
        when (color) {
            "White" -> "Weiß"
            "Orange" -> "Orange"
            "Green" -> "Grün"
            "Red" -> "Rot"
            "Blue" -> "Blau"
            "Yellow" -> "Gelb"
            "Purple" -> "Lila"
            "Black" -> "Schwarz"
            else -> color
        }
    } else {
        color
    }
}

private fun coinFillColor(coin: GameCoin): String = when (coin.color) {
    "White" -> "#ffffff"
    "Orange" -> "#f28a2e"
    "Green" -> "#48a868"
    "Red" -> "#d24b4b"
    "Blue" -> "#3f75d6"
    "Yellow" -> "#e9c938"
    "Purple" -> "#9258c9"
    "Black" -> "#2f3238"
    else -> "#d9d9d9"
}

private fun coinStrokeColor(coin: GameCoin): String = when (coin.color) {
    "White" -> "#9aa0a6"
    "Orange" -> "#aa4f12"
    "Green" -> "#1e6b3c"
    "Red" -> "#8c2323"
    "Blue" -> "#234489"
    "Yellow" -> "#8d7a14"
    "Purple" -> "#573082"
    "Black" -> "#0f1114"
    else -> "#7d7d7d"
}

private fun coinTextColor(coin: GameCoin): String = when (coin.color) {
    "White", "Yellow" -> "#1f2a1f"
    else -> "#ffffff"
}
