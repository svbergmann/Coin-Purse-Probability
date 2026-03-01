package com.svbergmann.coinpurseprobability

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoinPurseTest {

    @Test
    fun initialPurseHasExpectedCoins() {
        val purse = CoinPurse()
        assertEquals(8, purse.totalCoinCount())
        assertEquals(4, purse.countOf(GameCoin.WHITE_1))
        assertEquals(2, purse.countOf(GameCoin.WHITE_2))
        assertEquals(1, purse.countOf(GameCoin.WHITE_3))
        assertEquals(1, purse.countOf(GameCoin.ORANGE_1))
        assertEquals(0, purse.countOf(GameCoin.GREEN_1))
    }

    @Test
    fun drawReducesTotalAndAddsToDrawPile() {
        val purse = CoinPurse()
        val before = purse.totalCoinCount()

        purse.drawRandom(Random(1))

        assertEquals(before - 1, purse.totalCoinCount())
        assertEquals(1, purse.drawnCoins().size)
    }

    @Test
    fun probabilitiesSumToOneWhenNonEmpty() {
        val purse = CoinPurse()
        val sum = purse.probabilities().sumOf { it.probability }
        assertTrue(sum in 0.999999..1.000001)
    }

    @Test
    fun resetReturnsDrawnCoinsToPurse() {
        val purse = CoinPurse()
        purse.drawRandom(Random(2))
        purse.drawRandom(Random(3))

        purse.reset()

        assertEquals(8, purse.totalCoinCount())
        assertEquals(0, purse.drawnCoins().size)
    }

    @Test
    fun stateRoundTripRestoresPurse() {
        val source = CoinPurse()
        source.addCoin(GameCoin.ORANGE_1, 2)
        source.drawRandom(Random(4))
        source.drawRandom(Random(5))
        val exported = source.exportState()

        val restored = CoinPurse()
        val loaded = restored.importState(exported)

        assertTrue(loaded)
        assertEquals(source.totalCoinCount(), restored.totalCoinCount())
        assertEquals(source.remainingCoins(), restored.remainingCoins())
        assertEquals(source.drawnCoins(), restored.drawnCoins())
    }

    @Test
    fun resetToStarterPurseRestoresDefaultBag() {
        val purse = CoinPurse()
        purse.addCoin(GameCoin.RED_4, 2)
        purse.drawRandom(Random(7))

        purse.resetToStarterPurse()

        assertEquals(8, purse.totalCoinCount())
        assertEquals(4, purse.countOf(GameCoin.WHITE_1))
        assertEquals(2, purse.countOf(GameCoin.WHITE_2))
        assertEquals(1, purse.countOf(GameCoin.WHITE_3))
        assertEquals(1, purse.countOf(GameCoin.ORANGE_1))
        assertEquals(0, purse.drawnCoins().size)
    }

    @Test
    fun drawSpecificTracksProbabilityAtDraw() {
        val purse = CoinPurse()

        val drawn = purse.drawSpecific(GameCoin.WHITE_1)

        assertNotNull(drawn)
        assertEquals(GameCoin.WHITE_1, drawn.coin)
        assertEquals(4, drawn.numerator)
        assertEquals(8, drawn.denominator)
        assertEquals(0.5, drawn.probabilityAtDraw)
    }
}
