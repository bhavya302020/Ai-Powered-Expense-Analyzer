package com.sbi.processor.database;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(length = 500)
    private String description;
    
    @Column(length = 50)
    private String refNo;
    
    @Column
    private Double credit;
    
    @Column
    private Double debit;
    
    @Column
    private Double balance;
    
    @Column(length = 20)
    private String type;
    
    @Column
    private Double amount;
    
    @Column(length = 50)
    private String category;
    
    @Column(length = 200)
    private String sourceFile;
    
    @Column
    private Long createdAt;
    
    // Constructors
    public TransactionEntity() {}
    
    public TransactionEntity(LocalDate date, String description, String refNo,
                            Double credit, Double debit, Double balance,
                            String type, Double amount, String category, String sourceFile) {
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
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    
    public Double getCredit() { return credit; }
    public void setCredit(Double credit) { this.credit = credit; }
    
    public Double getDebit() { return debit; }
    public void setDebit(Double debit) { this.debit = debit; }
    
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}