package com.sbi.processor.api;

import com.sbi.processor.database.DatabaseService;
import com.sbi.processor.database.TransactionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    @Autowired
    private DatabaseService databaseService;
    
    // Get all transactions
    @GetMapping
    public ResponseEntity<?> getAllTransactions() {
        try {
            List<TransactionEntity> transactions = databaseService.getAllTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // Get transactions by date range
    @GetMapping("/date-range")
    public ResponseEntity<?> getByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<TransactionEntity> transactions = databaseService.getTransactionsByDateRange(start, end);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    
    // Get transactions by category
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getByCategory(@PathVariable String category) {
        try {
            List<TransactionEntity> transactions = databaseService.getTransactionsByCategory(category);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // Get all categories
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            List<String> categories = databaseService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // Get summary
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            Double totalCredits = databaseService.getTotalCredits(start, end);
            Double totalDebits = databaseService.getTotalDebits(start, end);
            List<TransactionEntity> transactions = databaseService.getTransactionsByDateRange(start, end);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalCredits", totalCredits);
            summary.put("totalDebits", totalDebits);
            summary.put("netFlow", totalCredits - totalDebits);
            summary.put("transactionCount", transactions.size());
            summary.put("startDate", start);
            summary.put("endDate", end);
            
            // Category breakdown
            Map<String, Long> categoryCount = transactions.stream()
                .collect(Collectors.groupingBy(TransactionEntity::getCategory, Collectors.counting()));
            summary.put("categoryBreakdown", categoryCount);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    
    // Get spending by category
    @GetMapping("/spending-by-category")
    public ResponseEntity<?> getSpendingByCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            List<TransactionEntity> transactions = databaseService.getTransactionsByDateRange(start, end);
            
            Map<String, Double> spending = transactions.stream()
                .filter(t -> t.getType().equals("Debit"))
                .collect(Collectors.groupingBy(
                    TransactionEntity::getCategory,
                    Collectors.summingDouble(TransactionEntity::getAmount)
                ));
            
            return ResponseEntity.ok(spending);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    
    // Get transaction count
    @GetMapping("/count")
    public ResponseEntity<?> getCount() {
        try {
            long count = databaseService.getTransactionCount();
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // Delete all transactions
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllTransactions() {
        try {
            databaseService.deleteAllTransactions();
            return ResponseEntity.ok("All transactions deleted");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}