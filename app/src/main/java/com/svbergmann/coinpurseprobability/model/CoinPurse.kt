package com.svbergmann.coinpurseprobability.model

import kotlinx.serialization.Serializable

@Serializable
class CoinPurse(
    private val coins: MutableMap<GameCoin, Int> = mutableMapOf(),
    private val drawnCoins: MutableList<GameCoin> = mutableListOf()
) {

    /**
     * Add an initial amount of certain coins to the purse.
     */
    init {
        addCoinToPurse(GameCoin.WHITE_VALUE_ONE, 4)
        addCoinToPurse(GameCoin.WHITE_VALUE_TWO, 2)
        addCoinToPurse(GameCoin.WHITE_VALUE_THREE, 1)
        addCoinToPurse(GameCoin.ORANGE_VALUE_ONE, 1)
        addCoinToPurse(GameCoin.GREEN_VALUE_ONE, 1)
    }

    /**
     * Add a specific type of coin to the purse.
     */
    fun addCoinToPurse(coin: GameCoin, quantity: Int = 1) {
        if (quantity <= 0) return
        coins[coin] = coins.getOrDefault(coin, 0) + quantity
        println(
            "$quantity x $coin added to purse. " +
                    "New count of '${coin}': ${coins[coin]}. " +
                    "New total: ${getTotalCoinCount()}."
        )
    }

    /**
     * Remove a specific type of coin from the purse without adding it to drawnCoins.
     * Returns true if successful, false if the coin (or enough quantity) was not found.
     */
    fun removeCoinFromPurse(gameCoin: GameCoin, quantity: Int = 1): Boolean {
        if (quantity <= 0) return false
        val currentCount = coins.getOrDefault(gameCoin, 0)
        if (currentCount < quantity) {
            println(
                "Not enough ${gameCoin.name} in purse to remove. " +
                        "Found: $currentCount, tried to remove: $quantity"
            )
            return false
        }
        val newCount = currentCount - quantity
        if (newCount == 0) {
            coins.remove(gameCoin)
            println("${gameCoin.name} removed from purse (all instances).")
        } else {
            coins[gameCoin] = newCount
            println("$quantity x ${gameCoin.name} removed from purse. Remaining: $newCount")
        }
        return true
    }

    /**
     * Get the total count of all individual coins in the purse.
     */
    fun getTotalCoinCount() = coins.values.sum()

    /**
     * Get the count of a specific type of coin.
     */
    fun getCountOfCoin(gameCoin: GameCoin) = coins.getOrDefault(gameCoin, 0)

    /**
     * Draw a random coin from the purse based on its quantity (weight)
     * and add it to drawnCoins.
     * Decrements the count of the drawn coin in the purse.
     *
     * @return The [GameCoin] that was drawn.
     * @throws NoSuchElementException if the purse is empty.
     */
    fun drawRandomCoin(): GameCoin {
        require(!coins.isEmpty()) { "Cannot draw from an empty purse." }

        // 1. Create a temporary list where each coin is repeated by its quantity.
        val expandedCoinList = coins.flatMap { (coinType, count) ->
            List(count) { coinType } // Creates a list containing 'coinType' repeated 'count' times
        }

        // 2. Pick a random coin from this expanded list.
        val drawnCoin = expandedCoinList.random()

        // 3. Decrement the count of the drawn coin in the original 'coins' map.
        val currentCount = coins[drawnCoin]!!
        if (currentCount == 1) {
            coins.remove(drawnCoin)
        } else {
            coins[drawnCoin] = currentCount - 1
        }
        println("Coin drawn from purse: $drawnCoin")
        println("Remaining ${drawnCoin}: ${coins.getOrDefault(drawnCoin, 0)}")

        // 4. Add the drawn coin to the 'drawnCoins' list.
        drawnCoins.add(drawnCoin)
        println("Drawn coins pile: $drawnCoins")

        return drawnCoin
    }

    /**
     * Reset the purse by returning all drawnCoins back to the main coins map.
     */
    fun resetPurse() {
        for (drawnCoin in drawnCoins) addCoinToPurse(drawnCoin, 1)
        drawnCoins.clear()
        println("Purse reset. Current state: ${getRemainingCoinsOverview()}")
    }

    /**
     * Get an overview of the remaining coins and their counts in the purse.
     */
    fun getRemainingCoinsOverview() = coins.toMap()

    /**
     * Get the list of coins that have been drawn.
     */
    fun getDrawnCoins() = drawnCoins.toList()

    /**
     * Get the remaining coins in the purse.
     */
    fun getRemainingCoins() = coins.toMap()

    /**
     * Calculates the probability of drawing each type of coin currently in the purse.
     *
     * @return A list of [CoinDrawProbability] objects, each representing a coin type
     *         and its probability of being drawn next. Returns an empty list if the
     *         purse is empty.
     */
    fun getCoinDrawProbabilities(): List<CoinDrawProbability> {
        val totalCoinsInPurse = getTotalCoinCount()
        if (totalCoinsInPurse == 0) {
            return emptyList()
        }

        return coins.map { (gameCoin, count) ->
            CoinDrawProbability(
                gameCoin = gameCoin,
                countInPurse = count,
                nom = count,
                denom = totalCoinsInPurse,
            )
        }.sortedByDescending { it.getProbabilityAsDouble() }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("═══ Coin Purse Status ═══")

        if (coins.isEmpty()) {
            sb.appendLine("No coins in the purse.")
        } else {
            val amountWidth = 8
            val colorWidth = 10
            val valueWidth = 7
            val costWidth = 7
            val probabilityWidth = 12

            val rowFormat =
                "| %-${amountWidth}s | %-${colorWidth}s | %-${valueWidth}s | %-${costWidth}s | %-${probabilityWidth}s |"

            val currentProbabilities = getCoinDrawProbabilities()

            sb.appendLine("Purse contains the following coins (${getTotalCoinCount()} total):")
            val divider = "+${"-".repeat(amountWidth + 2)}" +
                    "+${"-".repeat(colorWidth + 2)}" +
                    "+${"-".repeat(valueWidth + 2)}" +
                    "+${"-".repeat(costWidth + 2)}" +
                    "+${"-".repeat(probabilityWidth + 2)}" +
                    "+"
            sb.appendLine(divider)
            sb.appendLine(
                String.format(
                    rowFormat,
                    "Amount",
                    "Color",
                    "Value",
                    "Cost",
                    "Probability"
                )
            )
            sb.appendLine(divider)

            var sumValueWeighted = 0
            var sumCostWeighted = 0
            val sortedDisplayProbabilities =
                currentProbabilities.sortedBy { it.gameCoin.toString() }

            for (probInfo in sortedDisplayProbabilities) {
                val gameCoin = probInfo.gameCoin
                val count = probInfo.countInPurse

                sb.appendLine(
                    String.format(
                        rowFormat,
                        count,
                        gameCoin.coin.color,
                        gameCoin.coin.value,
                        gameCoin.coin.cost,
                        probInfo.getProbabilityAsString()
                    )
                )
                sumValueWeighted += count * gameCoin.coin.value
                sumCostWeighted += count * gameCoin.coin.cost
            }
            sb.appendLine("=".repeat(divider.length))
            val totalCoinCount = getTotalCoinCount()

            sb.appendLine(
                String.format(
                    rowFormat,
                    totalCoinCount,
                    " ",
                    "%.1f avg".format(sumValueWeighted.toDouble() / totalCoinCount),
                    "%.1f avg".format(sumCostWeighted.toDouble() / totalCoinCount),
                    "%.2f%%".format(currentProbabilities.sumOf { coinDrawProbability -> coinDrawProbability.getProbabilityAsDouble() } * 100)
                )
            )

            sb.appendLine(divider)
        }
        sb.appendLine()

        sb.appendLine("Drawn coins pile (${drawnCoins.size} total):")
        if (drawnCoins.isEmpty()) {
            sb.appendLine("- None -")
        } else {
            drawnCoins.forEachIndexed { index, gameCoin ->
                sb.appendLine("  ${index + 1}. $gameCoin")
            }
        }
        sb.appendLine("═════════════════════════")
        return sb.toString()
    }
}