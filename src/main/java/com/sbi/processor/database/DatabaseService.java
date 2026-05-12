package com.sbi.processor.database;

import com.sbi.processor.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // Save transaction to database
    public void saveTransaction(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity(
            transaction.getDate(),
            transaction.getDescription(),
            transaction.getRefNo(),
            transaction.getCredit(),
            transaction.getDebit(),
            transaction.getBalance(),
            transaction.getType(),
            transaction.getAmount(),
            transaction.getCategory(),
            transaction.getSourceFile()
        );
        
        transactionRepository.save(entity);
    }
    
    // Save multiple transactions
    public void saveTransactions(List<Transaction> transactions) {
        List<TransactionEntity> entities = transactions.stream()
            .map(t -> new TransactionEntity(
                t.getDate(),
                t.getDescription(),
                t.getRefNo(),
                t.getCredit(),
                t.getDebit(),
                t.getBalance(),
                t.getType(),
                t.getAmount(),
                t.getCategory(),
                t.getSourceFile()
            ))
            .collect(Collectors.toList());
        
        transactionRepository.saveAll(entities);
    }
    
    // Get all transactions
    public List<TransactionEntity> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    // Get transactions by date range
    public List<TransactionEntity> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByDateBetween(startDate, endDate);
    }
    
    // Get transactions by category
    public List<TransactionEntity> getTransactionsByCategory(String category) {
        return transactionRepository.findByCategory(category);
    }
    
    // Get all categories
    public List<String> getAllCategories() {
        return transactionRepository.findAllCategories();
    }
    
    // Get total credits
    public Double getTotalCredits(LocalDate startDate, LocalDate endDate) {
        Double total = transactionRepository.getTotalCredits(startDate, endDate);
        return total != null ? total : 0.0;
    }
    
    // Get total debits
    public Double getTotalDebits(LocalDate startDate, LocalDate endDate) {
        Double total = transactionRepository.getTotalDebits(startDate, endDate);
        return total != null ? total : 0.0;
    }
    
    // Delete all transactions
    public void deleteAllTransactions() {
        transactionRepository.deleteAll();
    }
    
    // Get transaction count
    public long getTransactionCount() {
        return transactionRepository.count();
    }
}