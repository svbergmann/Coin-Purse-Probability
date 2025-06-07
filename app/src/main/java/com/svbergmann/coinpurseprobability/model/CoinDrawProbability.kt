package com.svbergmann.coinpurseprobability.model

data class CoinDrawProbability(
    val gameCoin: GameCoin,
    val countInPurse: Int,
    val nom: Int,
    val denom: Int
) {
    fun getProbabilityAsDouble(): Double = nom * 1.0 / denom

    fun getProbabilityAsString(): String {
        return "%d/%d = %.3f".format(nom, denom, getProbabilityAsDouble())
    }
}
