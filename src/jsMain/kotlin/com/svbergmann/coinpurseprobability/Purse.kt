package com.svbergmann.coinpurseprobability

enum class GameCoin(
    val label: String,
    val color: String,
    val value: Int
) {
    WHITE_1("White 1", "White", 1),
    WHITE_2("White 2", "White", 2),
    WHITE_3("White 3", "White", 3),
    ORANGE_1("Orange 1", "Orange", 1),
    GREEN_1("Green 1", "Green", 1),
    GREEN_2("Green 2", "Green", 2),
    GREEN_4("Green 4", "Green", 4),
    RED_1("Red 1", "Red", 1),
    RED_2("Red 2", "Red", 2),
    RED_4("Red 4", "Red", 4),
    BLUE_1("Blue 1", "Blue", 1),
    BLUE_2("Blue 2", "Blue", 2),
    BLUE_4("Blue 4", "Blue", 4),
    YELLOW_1("Yellow 1", "Yellow", 1),
    YELLOW_2("Yellow 2", "Yellow", 2),
    YELLOW_4("Yellow 4", "Yellow", 4),
    PURPLE_1("Purple 1", "Purple", 1),
    BLACK_1("Black 1", "Black", 1)
}

data class CoinDrawProbability(
    val coin: GameCoin,
    val count: Int,
    val numerator: Int,
    val denominator: Int
) {
    val probability: Double
        get() = if (denominator == 0) 0.0 else numerator.toDouble() / denominator
}

data class DrawnCoin(
    val coin: GameCoin,
    val numerator: Int,
    val denominator: Int
) {
    val probabilityAtDraw: Double
        get() = if (denominator == 0) 0.0 else numerator.toDouble() / denominator
}

class CoinPurse {
    private val coins: MutableMap<GameCoin, Int> = mutableMapOf()
    private val drawnCoins: MutableList<DrawnCoin> = mutableListOf()

    init {
        resetToStarterPurse()
    }

    private fun addStarterCoins() {
        // Official starter bag: 4x white-1, 2x white-2, 1x white-3, 1x orange-1.
        addCoin(GameCoin.WHITE_1, 4)
        addCoin(GameCoin.WHITE_2, 2)
        addCoin(GameCoin.WHITE_3, 1)
        addCoin(GameCoin.ORANGE_1, 1)
    }

    fun addCoin(coin: GameCoin, quantity: Int = 1) {
        if (quantity <= 0) return
        coins[coin] = countOf(coin) + quantity
    }

    fun removeCoin(coin: GameCoin, quantity: Int = 1): Boolean {
        if (quantity <= 0) return false
        val current = countOf(coin)
        if (current < quantity) return false

        val newCount = current - quantity
        if (newCount == 0) {
            coins.remove(coin)
        } else {
            coins[coin] = newCount
        }
        return true
    }

    fun drawRandom(random: kotlin.random.Random = kotlin.random.Random.Default): DrawnCoin {
        val total = totalCoinCount()
        require(total > 0) { "Cannot draw from an empty purse." }

        val drawIndex = random.nextInt(total)
        var cursor = 0
        for ((coin, count) in coins) {
            cursor += count
            if (drawIndex < cursor) {
                return drawCoinInternal(coin, count, total)
            }
        }
        error("Draw failed although purse is not empty.")
    }

    fun drawSpecific(coin: GameCoin): DrawnCoin? {
        val total = totalCoinCount()
        if (total == 0) return null
        val count = countOf(coin)
        if (count == 0) return null
        return drawCoinInternal(coin, count, total)
    }

    private fun drawCoinInternal(coin: GameCoin, countBeforeDraw: Int, totalBeforeDraw: Int): DrawnCoin {
        removeCoin(coin, 1)
        val drawn = DrawnCoin(
            coin = coin,
            numerator = countBeforeDraw,
            denominator = totalBeforeDraw
        )
        drawnCoins += drawn
        return drawn
    }

    fun resetDrawn() {
        for (drawn in drawnCoins) {
            addCoin(drawn.coin, 1)
        }
        drawnCoins.clear()
    }

    fun resetToStarterPurse() {
        coins.clear()
        drawnCoins.clear()
        addStarterCoins()
    }

    fun reset() {
        resetDrawn()
    }

    fun totalCoinCount(): Int = coins.values.sum()

    fun countOf(coin: GameCoin): Int = coins[coin] ?: 0

    fun remainingCoins(): Map<GameCoin, Int> = coins.toMap()

    fun drawnCoins(): List<DrawnCoin> = drawnCoins.toList()

    fun probabilities(): List<CoinDrawProbability> {
        val total = totalCoinCount()
        if (total == 0) return emptyList()

        return coins.entries
            .map { (coin, count) ->
                CoinDrawProbability(
                    coin = coin,
                    count = count,
                    numerator = count,
                    denominator = total
                )
            }
            .sortedByDescending { it.probability }
    }

    fun exportState(): String {
        val coinsPart = GameCoin.entries
            .mapNotNull { coin ->
                val count = countOf(coin)
                if (count > 0) "${coin.name}=$count" else null
            }
            .joinToString(";")
        val drawnPart = drawnCoins.joinToString(";") { "${it.coin.name}:${it.numerator}:${it.denominator}" }
        return "$coinsPart|$drawnPart"
    }

    fun importState(raw: String): Boolean {
        val parts = raw.split("|", limit = 2)
        if (parts.size != 2) return false

        val parsedCoins = mutableMapOf<GameCoin, Int>()
        if (parts[0].isNotBlank()) {
            for (entry in parts[0].split(";")) {
                val keyValue = entry.split("=", limit = 2)
                if (keyValue.size != 2) return false
                val coin = runCatching { GameCoin.valueOf(keyValue[0]) }.getOrNull() ?: return false
                val count = keyValue[1].toIntOrNull() ?: return false
                if (count < 0) return false
                if (count > 0) {
                    parsedCoins[coin] = count
                }
            }
        }

        val parsedDrawn = mutableListOf<DrawnCoin>()
        if (parts[1].isNotBlank()) {
            for (entry in parts[1].split(";")) {
                val segments = entry.split(":", limit = 3)
                if (segments.size == 1) {
                    // Backward compatibility for old state format that only stored coin names.
                    val coin = runCatching { GameCoin.valueOf(segments[0]) }.getOrNull() ?: return false
                    parsedDrawn += DrawnCoin(coin, 0, 0)
                    continue
                }
                if (segments.size != 3) return false
                val coin = runCatching { GameCoin.valueOf(segments[0]) }.getOrNull() ?: return false
                val numerator = segments[1].toIntOrNull() ?: return false
                val denominator = segments[2].toIntOrNull() ?: return false
                if (numerator < 0 || denominator < 0) return false
                parsedDrawn += DrawnCoin(coin, numerator, denominator)
            }
        }

        coins.clear()
        coins.putAll(parsedCoins)
        drawnCoins.clear()
        drawnCoins.addAll(parsedDrawn)
        return true
    }
}
