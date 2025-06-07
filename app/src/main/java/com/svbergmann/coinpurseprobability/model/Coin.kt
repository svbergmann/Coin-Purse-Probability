package com.svbergmann.coinpurseprobability.model

import kotlinx.serialization.Serializable

@Serializable
data class Coin(
    val color: CoinColor,
    val value: Int,
    val cost: Int
) {
    override fun toString(): String {
        return "$color coin (Value: $value, Cost: $cost)"
    }
}

@Serializable
enum class CoinColor(private val lowerCase: String) {
    WHITE("white"), RED("red"), BLUE("blue"), GREEN("green"),
    YELLOW("yellow"), ORANGE("orange"), PURPLE("purple"), BLACK("black");

    override fun toString(): String = lowerCase
}

@Serializable
enum class GameCoin(internal val coin: Coin) {
    WHITE_VALUE_ONE(Coin(CoinColor.WHITE, 1, 0)),
    WHITE_VALUE_TWO(Coin(CoinColor.WHITE, 2, 0)),
    WHITE_VALUE_THREE(Coin(CoinColor.WHITE, 3, 0)),
    ORANGE_VALUE_ONE(Coin(CoinColor.ORANGE, 1, 3)),
    GREEN_VALUE_ONE(Coin(CoinColor.GREEN, 1, 3));

    override fun toString(): String = coin.toString()
}