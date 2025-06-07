package com.svbergmann.coinpurseprobability.viewmodel

import androidx.lifecycle.ViewModel
import com.svbergmann.coinpurseprobability.model.CoinDrawProbability
import com.svbergmann.coinpurseprobability.model.CoinPurse
import com.svbergmann.coinpurseprobability.model.GameCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CoinPurseViewModel: ViewModel() {
    private val _coinPurse = CoinPurse()
    val coinPurseInstance: CoinPurse
        get() = _coinPurse

    private val _probabilities = MutableStateFlow<List<CoinDrawProbability>>(emptyList())
    val probabilities: StateFlow<List<CoinDrawProbability>> = _probabilities.asStateFlow()

    init {
        updateProbabilities()
    }

    fun addCoin(gameCoin: GameCoin, quantity: Int = 1) {
        _coinPurse.addCoinToPurse(gameCoin, quantity)
        updateProbabilities()
    }

    fun drawRandom() : GameCoin{
        require(_coinPurse.getTotalCoinCount() > 0) {"Cannot draw from empty purse!"}
        val drawnCoin = _coinPurse.drawRandomCoin()
        updateProbabilities()
        return drawnCoin
    }

    fun reset() {
        _coinPurse.resetPurse()
        updateProbabilities()
    }

    private fun updateProbabilities() {
        _probabilities.value = _coinPurse.getCoinDrawProbabilities()
    }

    fun getPurseStateString(): String = _coinPurse.toString()
}