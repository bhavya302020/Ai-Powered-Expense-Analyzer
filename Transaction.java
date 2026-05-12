import java.time.LocalDate;

public class Transaction {
    private LocalDate date;
    private String description;
    private String refNo;
    private double credit;
    private double debit;
    private double balance;
    private String type;
    private double amount;
    private String category;
    private String sourceFile;

    public Transaction(LocalDate date, String description, String refNo,
                      double credit, double debit, double balance,
                      String type, double amount, String category, String sourceFile) {
        this.date = date;
        this.description = description;
        this.refNo = refNo;
        this.credit = credit;
        this.debit = debit;
        this.balance = balance;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.sourceFile = sourceFile;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public String getRefNo() { return refNo; }
    public double getCredit() { return credit; }
    public double getDebit() { return debit; }
    public double getBalance() { return balance; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getSourceFile() { return sourceFile; }

    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + date +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                '}';
    }
}