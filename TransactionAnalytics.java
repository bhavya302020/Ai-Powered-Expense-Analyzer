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

    public static List<CategorySummary> getCategoryAnalysis(List<Transaction> transactions) {
        double totalDebits = transactions.stream()
            .filter(t -> t.getType().equals("Debit"))
            .mapToDouble(Transaction::getDebit).sum();

        return transactions.stream()
            .filter(t -> t.getType().equals("Debit"))
            .collect(Collectors.groupingBy(Transaction::getCategory,
                    Collectors.summingDouble(Transaction::getAmount)))
            .entrySet().stream()
            .map(entry -> {
                CategorySummary summary = new CategorySummary();
                summary.category = entry.getKey();
                summary.totalAmount = entry.getValue();
                summary.percentage = (entry.getValue() / totalDebits) * 100;
                return summary;
            })
            .sorted((a, b) -> Double.compare(b.totalAmount, a.totalAmount))
            .collect(Collectors.toList());
    }

    public static double getNetFlow(List<Transaction> transactions) {
        return transactions.stream().mapToDouble(Transaction::getCredit).sum() -
               transactions.stream().mapToDouble(Transaction::getDebit).sum();
    }

    public static double getAverageDailyBalance(List<Transaction> transactions) {
        return transactions.stream().mapToDouble(Transaction::getBalance).average().orElse(0.0);
    }
}