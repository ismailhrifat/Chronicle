package com.chronicle.expense

import com.chronicle.expense.data.ACTION_INCREASE
import com.chronicle.expense.data.ACTION_INITIAL
import com.chronicle.expense.data.ACTION_REPAY
import com.chronicle.expense.data.DEBT_BORROWED
import com.chronicle.expense.data.DEBT_LENT
import com.chronicle.expense.data.Debt
import com.chronicle.expense.data.DebtLogic
import com.chronicle.expense.data.DebtRecord
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.data.balanceChanges
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtLogicTest {
    private fun rec(action: String, amount: Long) = DebtRecord(action = action, amount = amount, date = "2026-11-15")

    @Test fun repayClosesThenOverpayFlipsSide() {
        var d = DebtLogic.create(DEBT_LENT, "Rahim", "x", rec(ACTION_INITIAL, 1000))
        d = DebtLogic.append(d, rec(ACTION_REPAY, 1000))
        assertEquals(0, d.amount); assertEquals("closed", d.status); assertEquals(DEBT_LENT, d.type)
        d = DebtLogic.append(d, rec(ACTION_REPAY, 300))
        assertEquals(300, d.amount); assertEquals(DEBT_BORROWED, d.type); assertEquals("active", d.status)
        assertEquals(1300, DebtLogic.settled(d.records))
    }

    @Test fun editingARecordReplaysHistoryFromTheOriginalSide() {
        var d = DebtLogic.create(DEBT_BORROWED, "Karim", "x", rec(ACTION_INITIAL, 500))
        d = DebtLogic.append(d, rec(ACTION_INCREASE, 200))
        d = DebtLogic.append(d, rec(ACTION_REPAY, 100))
        assertEquals(600, d.amount); assertEquals(DEBT_BORROWED, d.type)
        val edited = DebtLogic.recalculate(d, d.records.toMutableList().also { it[0] = it[0].copy(amount = 50) })
        assertEquals(150, edited.amount); assertEquals(DEBT_BORROWED, edited.type)
        val removed = DebtLogic.recalculate(d, d.records.drop(1))
        // Without an initial record the replay starts from zero on the lent side, as the Debt app does.
        assertEquals(100, removed.amount); assertEquals(DEBT_LENT, removed.type)
    }

    @Test fun legacyDocumentsConvertTakaToMinorUnits() {
        val d = Debt.from(
            "abc",
            mapOf(
                "name" to "Rahim", "type" to "borrowed", "amount" to 1500.5, "originalAmount" to 2000L,
                "records" to listOf(mapOf("action" to "initial", "amount" to "2000", "account" to "bKash", "date" to "2025-01-02")),
            ),
            legacy = true,
        )
        assertEquals(150050, d.amount); assertEquals(200000, d.originalAmount)
        assertEquals(200000, d.records[0].amount); assertEquals(null, d.records[0].out); assertEquals(DEBT_BORROWED, d.type)
    }

    @Test fun debtEntriesMoveTheAccountBalance() {
        val out = Transaction(kind = TxKind.DEBT_OUT, amount = 700, accountId = "a")
        assertEquals(mapOf("a" to -700L), balanceChanges(null, out))
        assertEquals(mapOf("a" to 1000L), balanceChanges(out, out.copy(kind = TxKind.DEBT_IN, amount = 300)))
    }
}
