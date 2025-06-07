package com.svbergmann.coinpurseprobability.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CoinPurseTest : FunSpec({

    lateinit var coinPurse: CoinPurse

    beforeSpec {
        println("----- Starting CoinPurse Test Suite -----")
    }

    afterSpec {
        println("----- CoinPurse Test Suite Complete -----")
    }

    beforeEach {
        println("--- Setting up for test: '${it.name.testName}' ---")
        coinPurse = CoinPurse()
        println(coinPurse)
    }

    afterEach {
        println("--- Test '${it.a.name.testName}' finished with result: ${it.b.name} ---")
        println()
    }

    test("All coins should be added to the purse") {
        coinPurse.getTotalCoinCount() shouldBe 9
    }

    test("When removing a coin to the purse the total amount should be updated") {
        coinPurse.removeCoinFromPurse(GameCoin.WHITE_VALUE_ONE, 2)
        coinPurse.getTotalCoinCount() shouldBe 7
        println(coinPurse)
    }

    test("The initial amount of white coins with the value 1 should be 4") {
        coinPurse.getCountOfCoin(GameCoin.WHITE_VALUE_ONE) shouldBe 4
    }

    test("Drawing a coin should reduce the total count") {
        val initialCount = coinPurse.getTotalCoinCount()
        coinPurse.drawRandomCoin()
        coinPurse.getTotalCoinCount() shouldBe initialCount - 1
        println(coinPurse)
    }

    test("Drawing a random coin should have a somehow different outcome every executed test run") {
        val numberOfCycles = 10
        val initialTotalCount = coinPurse.getTotalCoinCount()

        repeat(numberOfCycles) {
            println("--- Cycle ${it + 1} of $numberOfCycles ---")

            val countBeforeDraw = coinPurse.getTotalCoinCount()
            require(countBeforeDraw > 0) { "Purse became empty unexpectedly during cycle ${it + 1}" }

            val randomCoin = coinPurse.drawRandomCoin()
            println("Random coin drawn: ${randomCoin.name}. Coins remaining: ${coinPurse.getTotalCoinCount()}")
            coinPurse.getTotalCoinCount() shouldBe countBeforeDraw - 1

            println("Resetting purse...")
            coinPurse.resetPurse()
            println("Purse reset. Total coins now: ${coinPurse.getTotalCoinCount()}")
            coinPurse.getTotalCoinCount() shouldBe initialTotalCount
        }
    }

    test("Empty the complete initial purse by drawing random coins.") {
        val initialTotalCount = coinPurse.getTotalCoinCount()

        repeat(initialTotalCount) {
            println("--- Cycle ${it + 1} of $initialTotalCount ---")

            val countBeforeDraw = coinPurse.getTotalCoinCount()
            require(countBeforeDraw > 0) { "Purse became empty unexpectedly during cycle ${it + 1}" }

            val randomCoin = coinPurse.drawRandomCoin()
            println("Random coin drawn: ${randomCoin}. Coins remaining: ${coinPurse.getTotalCoinCount()}")
            coinPurse.getTotalCoinCount() shouldBe countBeforeDraw - 1
            println(coinPurse)
        }
    }

})