package com.sbi.processor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.google.gson.*;

import javax.mail.*;
import javax.mail.search.*;
import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CombinedSBIProcessor {
    private List<Transaction> allTransactions;
    private Map<String, List<String>> categories;
    
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2})-(\\d{2})-(\\d{2})");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d+\\.\\d{2}");
    
    public CombinedSBIProcessor() {
        allTransactions = new ArrayList<>();
        initializeCategories();
    }
    
    private void initializeCategories() {
        categories = new HashMap<>();
        categories.put("Food & Dining", Arrays.asList("swiggy", "zomato", "restaurant", "paytm", "food", "cafe", "hotel", "dining"));
        categories.put("Shopping", Arrays.asList("amazon", "flipkart", "meesho", "myntra", "shopping", "mall", "store"));
        categories.put("Transport", Arrays.asList("uber", "ola", "cab", "irctc", "taxi", "metro", "bus", "petrol"));
        categories.put("Income", Arrays.asList("salary", "credit", "payme", "refund", "interest", "bonus"));
        categories.put("Utilities", Arrays.asList("electricity", "water", "gas", "mobile", "internet", "recharge"));
        categories.put("Other", Arrays.asList("other"));
    }
    
    public static void main(String[] args) {
        CombinedSBIProcessor processor = new CombinedSBIProcessor();
        processor.runCompleteAnalysis();
    }
    
    private void runCompleteAnalysis() {
        printHeader();
        
        try {
            boolean extractFromGmail = getUserYesNo("\n❓ Do you want to extract new statements from Gmail? (y/n): ");
            
            if (extractFromGmail) {
                System.out.println("\n📧 Gmail extraction (optional feature)");
            } else {
                System.out.println("\n⏭️ Skipping Gmail extraction.");
            }
            
            List<File> encryptedPDFs = findEncryptedPDFs();
            if (!encryptedPDFs.isEmpty()) {
                decryptPDFs(encryptedPDFs);
            }
            
            List<Transaction> transactions = processAllPDFs();
            
            if (transactions == null || transactions.isEmpty()) {
                System.out.println("\n❌ No transaction data to analyze. Exiting...");
                return;
            }
            
            String jsonFilename = saveResultsAsJSON(transactions);
            printDetailedSummary(transactions);
            printCompletionMessage(jsonFilename);
            
        } catch (Exception e) {
            System.out.println("\n❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== PDF DECRYPTION ====================
    
    private List<File> findEncryptedPDFs() {
        File currentDir = new File(System.getProperty("user.dir"));
        List<File> encryptedPDFs = new ArrayList<>();
        
        File[] pdfFiles = currentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        
        if (pdfFiles == null) {
            return encryptedPDFs;
        }
        
        for (File pdfFile : pdfFiles) {
            try (PDDocument document = PDDocument.load(pdfFile)) {
                if (document.isEncrypted()) {
                    encryptedPDFs.add(pdfFile);
                }
            } catch (Exception e) {
                encryptedPDFs.add(pdfFile);
            }
        }
        
        return encryptedPDFs;
    }
    
    private void decryptPDFs(List<File> encryptedPDFs) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔐 PDF DECRYPTION");
        System.out.println("=".repeat(70));
        System.out.println("\n📊 Found " + encryptedPDFs.size() + " encrypted PDF(s)");
        
        String password = getPDFPassword();
        if (password == null || password.isEmpty()) {
            System.out.println("❌ No password provided.");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("\n🔓 Starting decryption...");
        System.out.println("=".repeat(60));
        
        for (File pdfFile : encryptedPDFs) {
            try {
                System.out.println("\n📄 Processing: " + pdfFile.getName());
                
                if (decryptAndSavePDF(pdfFile, password)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                System.out.println("  ❌ Error: " + e.getMessage());
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Successfully decrypted: " + successCount);
        System.out.println("❌ Failed: " + failCount);
    }
    
    private boolean decryptAndSavePDF(File encryptedPDF, String password) {
        PDDocument document = null;
        
        try {
            try {
                document = PDDocument.load(encryptedPDF, password);
            } catch (Exception e) {
                document = PDDocument.load(encryptedPDF);
            }
            
            if (document == null) {
                return false;
            }
            
            String originalName = encryptedPDF.getName();
            String decryptedName = originalName.replace(".pdf", "_decrypted.pdf");
            
            PDDocument outputDoc = new PDDocument();
            try {
                document.getPages().forEach(page -> {
                    try {
                        outputDoc.addPage(page);
                    } catch (Exception ex) {
                        System.out.println("⚠️ Error: " + ex.getMessage());
                    }
                });
                
                outputDoc.save(new File(decryptedName));
                System.out.println("  ✅ Saved as: " + decryptedName);
                
                if (encryptedPDF.delete()) {
                    System.out.println("  🗑️ Deleted original");
                }
            } finally {
                outputDoc.close();
            }
            
            return true;
            
        } catch (Exception e) {
            System.out.println("  ❌ Decryption failed: " + e.getMessage());
            return false;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
    
    private String getPDFPassword() {
        System.out.println("\n🔐 PDF Decryption Password");
        System.out.println("=".repeat(50));
        System.out.println("\n📝 Password Format:");
        System.out.println("   Last 5 digits of mobile + DOB in DDMMYY");
        System.out.println("\n📌 Example: 67890150390");
        System.out.println("=".repeat(50));
        
        System.out.print("\n🔑 Enter password: ");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim();
    }
    
    // ==================== PDF PROCESSING ====================
    
    private List<Transaction> processAllPDFs() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌟 PDF ANALYSIS");
        System.out.println("=".repeat(70));
        
        File currentDir = new File(System.getProperty("user.dir"));
        File[] pdfFiles = currentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("\n⚠️ No PDF files found.");
            return null;
        }
        
        System.out.println("\n📂 Found " + pdfFiles.length + " PDF(s)");
        
        List<Transaction> allTransactions = new ArrayList<>();
        
        for (File pdfFile : pdfFiles) {
            try {
                List<Transaction> txns = processSinglePDF(pdfFile);
                allTransactions.addAll(txns);
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
        
        if (allTransactions.isEmpty()) {
            return null;
        }
        
        allTransactions.sort((t1, t2) -> t1.getDate().compareTo(t2.getDate()));
        return allTransactions;
    }
    
    private List<Transaction> processSinglePDF(File pdfFile) throws IOException {
        System.out.println("\n🔎 Processing: " + pdfFile.getName());
        List<Transaction> transactions = new ArrayList<>();
        
        List<String> lines = extractLinesFromPDF(pdfFile);
        if (lines.isEmpty()) {
            return transactions;
        }
        
        List<String> normalized = normalizeLines(lines);
        transactions = parseNormalizedLines(normalized, pdfFile.getName());
        
        System.out.println("✅ Parsed " + transactions.size() + " transactions");
        return transactions;
    }
    
    private List<String> extractLinesFromPDF(File pdfFile) throws IOException {
        List<String> lines = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            for (String line : text.split("\n")) {
                String cleaned = line.trim();
                if (!cleaned.isEmpty()) {
                    lines.add(cleaned);
                }
            }
        }
        
        return lines;
    }
    
    private List<String> normalizeLines(List<String> lines) {
        List<String> normalized = new ArrayList<>();
        
        for (String line : lines) {
            if (DATE_PATTERN.matcher(line).find() && AMOUNT_PATTERN.matcher(line).find()) {
                normalized.add(line);
            }
        }
        
        return normalized;
    }
    
    private List<Transaction> parseNormalizedLines(List<String> lines, String sourceFile) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (String line : lines) {
            try {
                String[] tokens = line.split("\\s+");
                if (tokens.length < 5) continue;
                
                LocalDate date = parseDate(tokens[0]);
                if (date == null) continue;
                
                double credit = parseAmount(tokens.length >= 4 ? tokens[tokens.length - 3] : "0");
                double debit = parseAmount(tokens.length >= 3 ? tokens[tokens.length - 2] : "0");
                double balance = parseAmount(tokens[tokens.length - 1]);
                
                String description = String.join(" ", Arrays.copyOfRange(tokens, 1, Math.max(1, tokens.length - 3)));
                String category = categorizeTransaction(description);
                double amount = credit > 0 ? credit : debit;
                
                if (amount == 0) continue;
                
                Transaction txn = new Transaction(
                    date, description, "", credit, debit, balance,
                    credit > 0 ? "Credit" : "Debit", amount, category, sourceFile
                );
                
                transactions.add(txn);
                
            } catch (Exception e) {
                continue;
            }
        }
        
        return transactions;
    }
    
    private LocalDate parseDate(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            if (parts.length != 3) return null;
            
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            
            if (year < 50) year += 2000;
            else year += 1900;
            
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
    
    private double parseAmount(String str) {
        try {
            return str == null || str.equals("-") ? 0.0 : Double.parseDouble(str.replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private String categorizeTransaction(String description) {
        String desc = description.toLowerCase();
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (desc.contains(keyword)) return entry.getKey();
            }
        }
        return "Other";
    }
    
    // ==================== JSON EXPORT ====================
    
    private String saveResultsAsJSON(List<Transaction> transactions) {
        try {
            System.out.println("\n💾 EXPORTING TO JSON");
            
            double totalCredits = transactions.stream().mapToDouble(Transaction::getCredit).sum();
            double totalDebits = transactions.stream().mapToDouble(Transaction::getDebit).sum();
            
            JsonObject root = new JsonObject();
            root.addProperty("total_transactions", transactions.size());
            root.addProperty("total_credits", totalCredits);
            root.addProperty("total_debits", totalDebits);
            root.addProperty("net_flow", totalCredits - totalDebits);
            
            JsonArray txnArray = new JsonArray();
            for (Transaction txn : transactions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("date", txn.getDate().toString());
                obj.addProperty("description", txn.getDescription());
                obj.addProperty("amount", txn.getAmount());
                obj.addProperty("category", txn.getCategory());
                txnArray.add(obj);
            }
            root.add("transactions", txnArray);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "transactions_" + timestamp + ".json";
            
            try (FileWriter writer = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(root, writer);
            }
            
            System.out.println("✅ Saved to: " + filename);
            return filename;
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            return null;
        }
    }
    
    // ==================== REPORTING ====================
    
    private void printDetailedSummary(List<Transaction> transactions) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 TRANSACTION SUMMARY");
        System.out.println("=".repeat(80));
        
        double totalCredits = transactions.stream().mapToDouble(Transaction::getCredit).sum();
        double totalDebits = transactions.stream().mapToDouble(Transaction::getDebit).sum();
        
        System.out.printf("Total Credits: ₹%,.2f%n", totalCredits);
        System.out.printf("Total Debits: ₹%,.2f%n", totalDebits);
        System.out.printf("Net Flow: ₹%,.2f%n", totalCredits - totalDebits);
        System.out.println("Transactions: " + transactions.size());
    }
    
    private void printCompletionMessage(String jsonFilename) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 ANALYSIS COMPLETED!");
        System.out.println("=".repeat(80));
        if (jsonFilename != null) {
            System.out.println("📁 Saved to: " + jsonFilename);
        }
    }
    
    private void printHeader() {
        System.out.println("=".repeat(80));
        System.out.println("🏦 SBI STATEMENT PROCESSOR");
        System.out.println("=".repeat(80));
    }
    
    private String getUserInput(String prompt) {
        System.out.print(prompt);
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
    
    private boolean getUserYesNo(String prompt) {
        String input = getUserInput(prompt);
        return input != null && input.toLowerCase().startsWith("y");
    }
}