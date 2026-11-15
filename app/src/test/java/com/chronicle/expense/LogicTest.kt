package com.chronicle.expense

import com.chronicle.expense.data.Money
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.data.balanceChanges
import com.chronicle.expense.notify.ReminderWorker
import com.chronicle.expense.sms.SmsPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val citySms = """
        23:20 14-Nov-26
        BDT 9,203.00 purchased at bdtickets
        CARD NO: 474***525
        Bal BDT 87,829.75
        CITY BANK
    """.trimIndent()

    @Test fun extractsAmountFromCityBankSample() {
        val p = SmsPattern.compile("BDT 9,203.00 purchased")!!
        assertEquals(920300L, p.extract(citySms))
    }

    @Test fun matchesDifferentAmountAndSpacing() {
        val p = SmsPattern.compile("BDT 9,203.00 purchased")!!
        assertEquals(45050L, p.extract("bdt  450.50  PURCHASED at shop\nBal BDT 1,000.00"))
        assertEquals(1250000L, p.extract("BDT 12,500 purchased at x"))
    }

    @Test fun ignoresBalanceLineAndUnrelatedMessages() {
        val p = SmsPattern.compile("BDT 9,203.00 purchased")!!
        assertNull(p.extract("Bal BDT 87,829.75 CITY BANK"))
        assertNull(p.extract("Your OTP is 123456"))
    }

    @Test fun prefixOnlyAndSuffixOnlySamples() {
        assertEquals(50000L, SmsPattern.compile("credited by BDT 500")!!.extract("Your A/C credited by BDT 500.00 on 1 Nov"))
        assertEquals(50000L, SmsPattern.compile("500 received")!!.extract("Tk 500 received from 017"))
    }

    @Test fun rejectsSampleWithoutNumberOrAnchor() {
        assertNull(SmsPattern.compile("purchased at"))
        assertNull(SmsPattern.compile("9,203.00"))
    }

    @Test fun regexCharactersInSampleAreLiteral() {
        val p = SmsPattern.compile("Tk. 100.00 (debited)")!!
        assertEquals(2550L, p.extract("A/C *123 Tk. 25.50 (debited) via POS"))
    }

    @Test fun senderMatching() {
        assertTrue(SmsPattern.senderMatches("CITY VISA", "CITY VISA"))
        assertTrue(SmsPattern.senderMatches("city visa", "CITYVISA"))
        assertTrue(SmsPattern.senderMatches("CITY", "AD-CITYBANK"))
        assertFalse(SmsPattern.senderMatches("CITY VISA", "bKash"))
        assertFalse(SmsPattern.senderMatches("", "bKash"))
    }

    @Test fun moneyRoundTrip() {
        assertEquals("৳9,203.00", Money.format(920300))
        assertEquals("−৳12.50", Money.format(-1250))
        assertEquals(920300L, Money.parse("9,203.00"))
        assertEquals(920350L, Money.parse("9203.5"))
        assertNull(Money.parse("abc"))
        assertEquals("9203.5", Money.toInput(920350))
    }

    @Test fun balanceChangesForAddEditDeleteTransfer() {
        val expense = Transaction(id = "1", kind = TxKind.EXPENSE, amount = 500, accountId = "a")
        assertEquals(mapOf("a" to -500L), balanceChanges(null, expense))
        assertEquals(mapOf("a" to 500L), balanceChanges(expense, null))

        val edited = expense.copy(amount = 300, accountId = "b")
        assertEquals(mapOf("a" to 500L, "b" to -300L), balanceChanges(expense, edited))

        val toIncome = expense.copy(kind = TxKind.INCOME)
        assertEquals(mapOf("a" to 1000L), balanceChanges(expense, toIncome))

        val transfer = Transaction(kind = TxKind.TRANSFER, amount = 200, accountId = "a", toAccountId = "b")
        assertEquals(mapOf("a" to -200L, "b" to 200L), balanceChanges(null, transfer))
        assertTrue(balanceChanges(expense, expense.copy(description = "x")).isEmpty())
    }

    @Test fun reminderDates() {
        val nov14 = LocalDate.of(2026, 11, 14)
        assertEquals(LocalDate.of(2026, 11, 20), ReminderWorker.nextOccurrence(20, nov14))
        assertEquals(nov14, ReminderWorker.nextOccurrence(14, nov14))
        assertEquals(LocalDate.of(2026, 12, 5), ReminderWorker.nextOccurrence(5, nov14))
        assertEquals(LocalDate.of(2027, 2, 28), ReminderWorker.nextOccurrence(31, LocalDate.of(2027, 2, 1)))
    }
}
