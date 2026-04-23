package com.act.web.controller;


import com.act.model.InvoiceDetail;
import com.act.model.InvoiceMaster;
import com.act.model.JournalEntry;
import com.act.model.Transaction;
import com.act.repo.LedgerRepository;
import com.act.repo.TrasactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Component
public class TransactionUtil {

    @Autowired
    TrasactionRepository trasactionRepository;

    @Autowired
    LedgerRepository ledgerRepository;



    public void updateBalanceandTransaction(BigDecimal totalAmount,
                                            InvoiceMaster invoiceMaster,
                                            InvoiceDetail invoiceDetail,
                                            JournalEntry journalEntry,
                                            String from,
                                            String to,
                                            String desc,
                                            LocalDate transactionDate,
                                            boolean skipdeleteTransactions) {
        if (!skipdeleteTransactions) {
            Optional<List<Transaction>> retJeList = null;
            if (invoiceMaster != null && invoiceMaster.getId() != null) {
                retJeList = trasactionRepository.findByInvoiceMaster(invoiceMaster);
            }
            if (journalEntry != null) {
                retJeList = trasactionRepository.findByJournalEntry(journalEntry);
            }

            if (retJeList != null && retJeList.isPresent()) {
                Iterator<Transaction> ita = retJeList.get().iterator();
                while (ita.hasNext()) {
                    trasactionRepository.deleteById(ita.next().getId());
                }
            }
        }
        Transaction transaction = new Transaction();
        transaction.setAccount(ledgerRepository.findByLedgerName(from).get());
        transaction.setAmount(totalAmount.multiply(new BigDecimal(-1)));
        transaction.setDescription(desc + " : flow : "+ from + " ==> " + to);
        transaction.setTransactionDate(transactionDate);
        transaction.setInvoiceMaster(invoiceMaster);
        transaction.setJournalEntry(journalEntry);
        transaction.setInvoiceDetail(invoiceDetail);
        trasactionRepository.save(transaction);

        Transaction transactionNew = new Transaction();
        transactionNew.setAccount(ledgerRepository.findByLedgerName(to).get());
        transactionNew.setAmount(totalAmount);
        transactionNew.setDescription(desc + " : flow : "+ from + " ==> " + to);
        transactionNew.setTransactionDate(transactionDate);
        transactionNew.setInvoiceMaster(invoiceMaster);
        transactionNew.setJournalEntry(journalEntry);
        transaction.setInvoiceDetail(invoiceDetail);
        trasactionRepository.save(transactionNew);
    }
}
