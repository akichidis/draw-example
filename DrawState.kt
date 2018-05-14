package com.draws.state

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class DrawState(val index: String,
                     val oracle: Party,
                     val drawParticipants: List<Participant>): ContractState {

    override val participants: List<AbstractParty> get() = drawParticipants.map { p -> p.party }


    class StockIndexPricesCommand(val index: String, val prices: String): CommandData

    /* Every participant is associated with a public key & ticketId for the draw */
    data class Participant(val party: Party,
                           val ticketId: String) {
    }
}