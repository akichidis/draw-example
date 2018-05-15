package com.draws.contract

import com.draws.state.DrawState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction
import java.math.BigInteger
import java.util.Collections.sort

open class DrawContract : Contract {
    companion object {
        @JvmStatic
        val DRAW_CONTRACT_ID = "com.draws.contract.DrawContract"
    }

    override fun verify(tx: LedgerTransaction) {

        //Used on the draw creation transaction
        if (!tx.outputStates.isEmpty()) {
            requireThat {
                "Only one draw output state should be created" using (tx.outputsOfType<DrawState>().size == 1)

                val drawState = tx.outputsOfType<DrawState>().first()

                "Duplicate ticket ids found " using containsUniqueTickets(drawState)
            }
        }

        //Otherwise, someone is trying to spend the draw state
        requireThat {
            val drawState = tx.inRefsOfType<DrawState>().first()
            val stateData = drawState.state.data

            //Require to have only 1 command of that type
            val command = tx.commands.requireSingleCommand<DrawState.StockIndexPricesCommand>()
            val oraclePublicKey = stateData.oracle.owningKey

            "Provided stock stockIndex data is wrong" using (command.value.stockIndex == stateData.stockIndex)
            "Command signer is other than state's oracle" using (command.signers.contains(oraclePublicKey))

            val seed = command.value.prices

            //Hash the prices concatenated with every user's ticket id
            val participantResults = stateData.drawParticipants.map { p -> Pair(p, toInt(SecureHash.sha256(p.ticketId + seed))) }

            //Sort based on the hash int results
            sort(participantResults, { p1, p2 -> p1.second.compareTo(p2.second) })

            val winner = participantResults.last()

            "Only the winner can spend the state" using command.signers.contains(winner.first.party.owningKey)
        }
    }

    private fun toInt(hash: SecureHash): BigInteger {
        return BigInteger(hash.toString(), 16).abs()
    }

    private fun containsUniqueTickets(drawState: DrawState): Boolean {
        return drawState.drawParticipants.map { p -> p.ticketId }.toSet().size == drawState.drawParticipants.size
    }
}