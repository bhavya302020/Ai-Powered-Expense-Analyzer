package com.sbi.processor;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionAnalytics {
    
    public static class MonthlySummary {
        public String month;
        public int transactionCount;
        public double totalCredits;
        public double totalDebits;
        public double netFlow;
    }

    public static class CategorySummary {
        public String category;
        public int count;
        public double totalAmount;
        public double percentage;
    }

    public static List<MonthlySummary> getMonthlyAnalysis(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(t -> t.getDate().getYear() + "-" + 
                    String.format("%02d", t.getDate().getMonthValue())))
            .entrySet().stream()
            .map(entry -> {
                MonthlySummary summary = new MonthlySummary();
                summary.month = entry.getKey();
                summary.transactionCount = entry.getValue().size();
                summary.totalCredits = entry.getValue().stream()
                    .mapToDouble(Transaction::getCredit).sum();
                summary.totalDebits = entry.getValue().stream()
                    .mapToDouble(Transaction::getDebit).sum();
                summary.netFlow = summary.totalCredits - summary.totalDebits;
                return summary;
            })
            .collect(Collectors.toList());
    }

    public static double getNetFlow(List<Transaction> transactions) {
        return transactions.stream().mapToDouble(Transaction::getCredit).sum() -
               transactions.stream().mapToDouble(Transaction::getDebit).sum();
    }
}