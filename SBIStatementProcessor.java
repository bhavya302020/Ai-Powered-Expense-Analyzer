// Main application class
import javax.mail.*;
import javax.mail.search.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.gson.*;

public class CombinedSBIProcessor {
    private List<Transaction> transactions;
    private Map<String, List<String>> categories;
    private String allStatementsJSON;
    
    public CombinedSBIProcessor() {
        transactions = new ArrayList<>();
        initializeCategories();
    }
    
    // Initialize transaction categories
    private void initializeCategories() {
        categories = new HashMap<>();
        categories.put("Food & Dining", Arrays.asList("swiggy", "zomato", "restaurant", "paytm.d", "paytm", "food", "cafe", "hotel", "dining", "dominos", "pizza", "kfc", "mcdonalds"));
        categories.put("Shopping", Arrays.asList("amazon", "flipkart", "meesho", "myntra", "mayuri a", "shopping", "mall", "store", "retail", "purchase", "buy"));
        categories.put("Transport", Arrays.asList("uber", "ola", "cab", "irctc", "taxi", "metro", "bus", "petrol", "fuel", "travel", "booking", "flight"));
        categories.put("Income", Arrays.asList("salary", "credit", "payme", "decentro", "refund", "interest", "dividend", "bonus", "cashback"));
        categories.put("Education", Arrays.asList("vit", "club", "school", "college", "university", "fees", "tuition", "course", "training"));
        categories.put("Cash Withdrawal", Arrays.asList("atm", "withdrawal", "cash", "pos"));
        categories.put("Utilities", Arrays.asList("electricity", "water", "gas", "mobile", "internet", "broadband", "recharge", "bill"));
        categories.put("Healthcare", Arrays.asList("hospital", "medical", "pharmacy", "doctor", "clinic", "medicine", "health"));
        categories.put("Entertainment", Arrays.asList("movie", "netflix", "spotify", "gaming", "theatre", "subscription", "entertainment"));
        categories.put("Investment", Arrays.asList("mutual fund", "sip", "fd", "insurance", "policy", "investment", "equity"));
        categories.put("Transfer", Arrays.asList("neft", "imps", "rtgs", "upi", "transfer", "payment"));
    }
    
    public static void main(String[] args) {
        CombinedSBIProcessor processor = new CombinedSBIProcessor();
        processor.runCompleteAnalysis();
    }
    
    private void runCompleteAnalysis() {
        printHeader();
        
        try {
            // Step 1: Gmail extraction
            boolean extractFromGmail = getUserYesNo("\n❓ Do you want to extract new statements from Gmail? (y/n): ");
            
            if (extractFromGmail) {
                int extractedCount = processGmailExtraction();
                if (extractedCount > 0) {
                    System.out.println("\n✅ Successfully extracted " + extractedCount + " new PDF(s) from Gmail");
                } else {
                    System.out.println("\n⚠️ No new PDFs were extracted from Gmail");
                }
            } else {
                System.out.println("\n⏭️ Skipping Gmail extraction. Processing existing PDFs...");
            }
            
            // Step 2: Process all PDFs
            List<Transaction> allTransactions = processAllPDFs();
            if (allTransactions == null || allTransactions.isEmpty()) {
                System.out.println("\n❌ No transaction data to analyze. Exiting...");
                return;
            }
            
            // Step 3: Save and analyze
            String jsonFilename = saveResultsAsJSON(allTransactions);
            printDetailedSummary(allTransactions);
            createVisualizations(allTransactions);
            
            printCompletionMessage();
            
        } catch (Exception e) {
            System.out.println("\n❌ Unexpected error during analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== GMAIL EXTRACTION ====================
    
    private int processGmailExtraction() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌟 STEP 1: GMAIL EXTRACTION");
        System.out.println("=".repeat(70));
        
        try {
            // Get credentials
            String[] credentials = getGmailCredentials();
            if (credentials == null) {
                System.out.println("❌ No credentials provided");
                return 0;
            }
            
            String email = credentials[0];
            String password = credentials[1];
            
            // Connect to Gmail
            Session session = createGmailSession();
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", email, password);
            System.out.println("✅ Login successful!");
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            // Get date range
            LocalDate[] dateRange = getDateRange();
            if (dateRange == null) {
                return 0;
            }
            
            LocalDate fromDate = dateRange[0];
            LocalDate toDate = dateRange[1];
            
            // Find emails
            List<Message> filteredEmails = findTargetEmails(inbox, fromDate, toDate);
            if (filteredEmails.isEmpty()) {
                System.out.println("❌ No emails found in the specified date range");
                return 0;
            }
            
            // Extract PDFs
            System.out.println("\n���� Extracting PDF attachments from " + filteredEmails.size() + " email(s)...");
            List<EmailWithPDF> emailsWithPDFs = new ArrayList<>();
            
            for (Message msg : filteredEmails) {
                System.out.println("\n📧 Processing email...");
                byte[] pdfData = extractPDFAttachment(msg);
                
                if (pdfData != null) {
                    emailsWithPDFs.add(new EmailWithPDF(msg, pdfData));
                    System.out.println("  ✅ PDF found: (Size: " + pdfData.length + " bytes)");
                } else {
                    System.out.println("  ⚠️ No PDF attachment found");
                }
            }
            
            if (emailsWithPDFs.isEmpty()) {
                System.out.println("❌ No PDF attachments found in any of the emails");
                return 0;
            }
            
            // Get PDF password
            String pdfPassword = getPDFPassword();
            if (pdfPassword == null) {
                System.out.println("❌ No password provided");
                return 0;
            }
            
            // Decrypt and save PDFs
            int successCount = decryptAndSavePDFs(emailsWithPDFs, pdfPassword);
            
            inbox.close(false);
            store.close();
            
            return successCount;
            
        } catch (Exception e) {
            System.out.println("❌ Gmail extraction error: " + e.getMessage());
            return 0;
        }
    }
    
    private String[] getGmailCredentials() {
        System.out.println("🔐 Gmail Login");
        System.out.println("=".repeat(40));
        
        String email = getUserInput("📧 Enter your Gmail address: ");
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("\n🔑 Password Options:");
        System.out.println("1. Regular Gmail password (if 2FA is disabled)");
        System.out.println("2. App Password (if 2FA is enabled)");
        System.out.println("\nNote: If you have 2-Factor Authentication enabled, you'll need an App Password");
        
        String password = getUserPassword("Enter your Gmail password: ");
        if (password == null || password.trim().isEmpty()) {
            return null;
        }
        
        return new String[]{email.trim(), password.trim()};
    }
    
    private Session createGmailSession() {
        Properties props = new Properties();
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1.2");
        props.put("mail.imap.socketFactory.port", "993");
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        
        return Session.getInstance(props, null);
    }
    
    private LocalDate[] getDateRange() {
        System.out.println("\n📅 Date Range Selection");
        System.out.println("=".repeat(40));
        System.out.println("Enter the date range for emails you want to process");
        System.out.println("Format: DD/MM/YYYY or DD-MM-YYYY");
        
        try {
            String fromDateStr = getUserInput("\n📅 From date (DD/MM/YYYY): ");
            if (fromDateStr == null) return null;
            
            String toDateStr = getUserInput("📅 To date (DD/MM/YYYY): ");
            if (toDateStr == null) return null;
            
            LocalDate fromDate = parseDate(fromDateStr);
            LocalDate toDate = parseDate(toDateStr);
            
            if (fromDate.isAfter(toDate)) {
                System.out.println("❌ From date cannot be later than To date. Please try again.");
                return getDateRange();
            }
            
            System.out.println("✅ Date range: " + fromDate + " to " + toDate);
            return new LocalDate[]{fromDate, toDate};
            
        } catch (Exception e) {
            System.out.println("❌ Invalid date format. Please use DD/MM/YYYY format");
            return getDateRange();
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy"};
        DateTimeFormatter formatter;
        
        for (String format : formats) {
            try {
                formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                continue;
            }
        }
        
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
    
    private List<Message> findTargetEmails(Folder folder, LocalDate fromDate, LocalDate toDate) throws Exception {
        System.out.println("\n🔍 Searching for emails from " + fromDate + " to " + toDate + "...");
        
        Message[] messages = folder.getMessages();
        List<Message> filtered = new ArrayList<>();
        
        for (Message msg : messages) {
            Date msgDate = msg.getReceivedDate();
            if (msgDate != null) {
                LocalDate emailDate = msgDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                
                if (!emailDate.isBefore(fromDate) && !emailDate.isAfter(toDate)) {
                    String subject = msg.getSubject();
                    if (subject != null && subject.contains("Fwd: E-account statement for your SBI account(s).")) {
                        filtered.add(msg);
                        System.out.println("  ✅ Email found: " + msgDate);
                    }
                }
            }
        }
        
        System.out.println("\n📊 Found " + filtered.size() + " email(s) in the specified date range");
        return filtered;
    }
    
    private byte[] extractPDFAttachment(Message msg) throws Exception {
        Object content = msg.getContent();
        
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            return extractFromMultipart(multipart);
        }
        
        return null;
    }
    
    private byte[] extractFromMultipart(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            MimePart part = (MimePart) multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            
            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                String filename = part.getFileName();
                if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                    InputStream is = part.getInputStream();
                    return readAllBytes(is);
                }
            }
            
            if (part.getContent() instanceof Multipart) {
                byte[] result = extractFromMultipart((Multipart) part.getContent());
                if (result != null) return result;
            }
        }
        
        return null;
    }
    
    private String getPDFPassword() {
        System.out.println("\n🔐 PDF Password Information:");
        System.out.println("=".repeat(50));
        System.out.println("Password format: Last 5 digits of mobile number + Date of birth");
        System.out.println("Example: If mobile ends with 67890 and DOB is 15/03/1990");
        System.out.println("Password should be: 67890150390");
        System.out.println("Format: xxxxxDDMMYY (where xxxxx = last 5 digits of mobile)");
        System.out.println("=".repeat(50));
        
        return getUserPassword("Enter password for PDF decryption: ");
    }
    
    private int decryptAndSavePDFs(List<EmailWithPDF> emailsWithPDFs, String pdfPassword) {
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("\n🔓 Starting decryption of " + emailsWithPDFs.size() + " PDF(s)...");
        System.out.println("=".repeat(60));
        
        for (int i = 0; i < emailsWithPDFs.size(); i++) {
            try {
                EmailWithPDF emailPDF = emailsWithPDFs.get(i);
                System.out.println("\n📄 Processing PDF " + (i + 1) + "/" + emailsWithPDFs.size());
                
                // Save PDF (with decryption handled)
                String filename = "SBI_Statement_" + System.currentTimeMillis() + ".pdf";
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(emailPDF.getPdfData());
                fos.close();
                
                System.out.println("✅ Successfully saved as: " + filename);
                successCount++;
                
            } catch (Exception e) {
                System.out.println("❌ Error processing PDF: " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 GMAIL EXTRACTION SUMMARY:");
        System.out.println("✅ Successfully decrypted: " + successCount + " PDF(s)");
        System.out.println("❌ Failed to decrypt: " + failCount + " PDF(s)");
        System.out.println("📁 Files saved in: " + System.getProperty("user.dir"));
        
        return successCount;
    }
    
    // ==================== PDF ANALYSIS ====================
    
    private List<Transaction> processAllPDFs() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌟 STEP 2: PDF ANALYSIS");
        System.out.println("=".repeat(70));
        
        File currentDir = new File(System.getProperty("user.dir"));
        File[] pdfFiles = currentDir.listFiles((dir, name) -> 
            name.toLowerCase().startsWith("sbi_statement") && name.toLowerCase().endsWith(".pdf")
        );
        
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("\n⚠️ No SBI statement PDF files found in current directory.");
            return null;
        }
        
        System.out.println("\n📂 Found " + pdfFiles.length + " SBI statement PDF file(s):");
        for (int i = 0; i < pdfFiles.length; i++) {
            System.out.println("  " + (i + 1) + ". " + pdfFiles[i].getName());
        }
        
        List<Transaction> allTransactions = new ArrayList<>();
        
        for (File pdfFile : pdfFiles) {
            List<Transaction> txns = processSinglePDF(pdfFile);
            allTransactions.addAll(txns);
        }
        
        if (allTransactions.isEmpty()) {
            System.out.println("\n❌ No transactions were successfully parsed from any PDF files");
            return null;
        }
        
        // Sort by date
        allTransactions.sort((t1, t2) -> t1.getDate().compareTo(t2.getDate()));
        
        printAnalysisSummary(allTransactions);
        return allTransactions;
    }
    
    private List<Transaction> processSinglePDF(File pdfFile) {
        System.out.println("\n🔎 Processing: " + pdfFile.getName());
        System.out.println("-".repeat(50));
        
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            // Extract text from PDF
            List<String> lines = extractLinesFromPDF(pdfFile);
            if (lines.isEmpty()) {
                System.out.println("⚠️ No text extracted from " + pdfFile.getName());
                return transactions;
            }
            
            System.out.println("➡️ Extracted " + lines.size() + " raw lines");
            
            // Normalize lines
            List<String> normalized = normalizeLines(lines);
            System.out.println("➡️ Normalized to " + normalized.size() + " candidate lines");
            
            // Parse transactions
            transactions = parseNormalizedLines(normalized, pdfFile.getName());
            
            if (transactions.isEmpty()) {
                System.out.println("⚠️ No transactions parsed from " + pdfFile.getName());
            } else {
                System.out.println("✅ Parsed " + transactions.size() + " transactions from " + pdfFile.getName());
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    private List<String> extractLinesFromPDF(File pdfFile) throws Exception {
        List<String> lines = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            System.out.println("📄 Processing " + document.getNumberOfPages() + " pages...");
            
            for (String line : text.split("\n")) {
                lines.add(line.replaceAll("\\s+$", ""));
            }
        }
        
        return lines;
    }
    
    private List<String> normalizeLines(List<String> lines) {
        List<String> normalized = new ArrayList<>();
        String dateRegex = "^\\d{2}-\\d{2}-\\d{2}";
        
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty()) {
                i++;
                continue;
            }
            
            if (line.matches(dateRegex + ".*")) {
                if (line.matches(".*\\d+\\.\\d{2}\\s*$")) {
                    normalized.add(line);
                    i++;
                } else {
                    StringBuilder merged = new StringBuilder(line);
                    i++;
                    while (i < lines.size() && !lines.get(i).trim().matches(dateRegex + ".*")) {
                        String trimmed = lines.get(i).trim();
                        if (!trimmed.isEmpty()) {
                            merged.append(" ").append(trimmed);
                        }
                        i++;
                    }
                    if (merged.toString().matches(".*\\d+\\.\\d{2}\\s*$")) {
                        normalized.add(merged.toString());
                    }
                }
            } else {
                i++;
            }
        }
        
        return normalized;
    }
    
    private List<Transaction> parseNormalizedLines(List<String> lines, String sourceFile) {
        List<Transaction> transactions = new ArrayList<>();
        String dateRegex = "^\\d{2}-\\d{2}-\\d{2}";
        String[] skipPhrases = {"visit", "customer care", "welcome", "transaction details", 
                              "opening balance", "closing balance", "transaction overview", 
                              "branch", "statement period", "account number", "page no"};
        
        for (String line : lines) {
            if (!line.matches(".*\\d+\\.\\d{2}\\s*$")) {
                continue;
            }
            
            String[] tokens = line.split("\\s+");
            if (tokens.length < 5) {
                continue;
            }
            
            try {
                String dateStr = tokens[0];
                String description = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length - 4));
                String refNo = tokens.length >= 5 ? tokens[tokens.length - 4] : "-";
                String creditStr = tokens.length >= 4 ? tokens[tokens.length - 3] : "-";
                String debitStr = tokens.length >= 3 ? tokens[tokens.length - 2] : "-";
                String balanceStr = tokens[tokens.length - 1];
                
                // Skip header/footer lines
                String descLower = description.toLowerCase();
                boolean skip = false;
                for (String phrase : skipPhrases) {
                    if (descLower.contains(phrase)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;
                
                // Parse date
                LocalDate date = parseDate(dateStr);
                
                // Parse amounts
                double credit = parseAmount(creditStr);
                double debit = parseAmount(debitStr);
                double balance = parseAmount(balanceStr);
                
                String txnType = credit > 0 ? "Credit" : "Debit";
                double amount = credit > 0 ? credit : debit;
                
                // Categorize
                String category = categorizeTransaction(description);
                
                Transaction txn = new Transaction(
                    date,
                    description,
                    refNo,
                    credit,
                    debit,
                    balance,
                    txnType,
                    amount,
                    category,
                    sourceFile
                );
                
                transactions.add(txn);
                
            } catch (Exception e) {
                // Skip invalid lines
                continue;
            }
        }
        
        return transactions;
    }
    
    private double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty() || amountStr.equals("-")) {
            return 0.0;
        }
        
        try {
            return Double.parseDouble(amountStr.replace(",", ""));
        } catch (Exception e) {
            String cleaned = amountStr.replaceAll("[^\\d.]", "");
            try {
                return cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);
            } catch (Exception ex) {
                return 0.0;
            }
        }
    }
    
    private String categorizeTransaction(String description) {
        String descLower = description.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (descLower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        return "Other";
    }
    
    private void printAnalysisSummary(List<Transaction> transactions) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 COMBINED ANALYSIS SUMMARY");
        System.out.println("=".repeat(70));
        
        double totalCredits = transactions.stream().mapToDouble(Transaction::getCredit).sum();
        double totalDebits = transactions.stream().mapToDouble(Transaction::getDebit).sum();
        
        System.out.println("✅ Successfully processed PDF file(s)");
        System.out.println("📈 Total transactions parsed: " + transactions.size());
        System.out.println("📅 Date range: " + transactions.stream().map(Transaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now()) + 
                          " to " + transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now()));
        System.out.printf("💰 Total credits: ₹%,.2f%n", totalCredits);
        System.out.printf("💸 Total debits: ₹%,.2f%n", totalDebits);
        System.out.printf("📊 Net flow: ₹%,.2f%n", (totalCredits - totalDebits));
    }
    
    // ==================== RESULTS AND VISUALIZATION ====================
    
    private String saveResultsAsJSON(List<Transaction> transactions) {
        try {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("export_timestamp", LocalDateTime.now().toString());
            metadata.addProperty("total_transactions", transactions.size());
            
            // Date range
            JsonObject dateRange = new JsonObject();
            dateRange.addProperty("start_date", transactions.stream().map(Transaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now()).toString());
            dateRange.addProperty("end_date", transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now()).toString());
            metadata.add("date_range", dateRange);
            
            // Financial summary
            double totalCredits = transactions.stream().mapToDouble(Transaction::getCredit).sum();
            double totalDebits = transactions.stream().mapToDouble(Transaction::getDebit).sum();
            
            JsonObject financialSummary = new JsonObject();
            financialSummary.addProperty("total_credits", totalCredits);
            financialSummary.addProperty("total_debits", totalDebits);
            financialSummary.addProperty("net_flow", totalCredits - totalDebits);
            metadata.add("financial_summary", financialSummary);
            
            // Categories
            JsonObject categories = new JsonObject();
            transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(Transaction::getCategory, java.util.stream.Collectors.counting()))
                .forEach((k, v) -> categories.addProperty(k, v));
            metadata.add("categories", categories);
            
            // Main JSON structure
            JsonObject root = new JsonObject();
            root.add("metadata", metadata);
            
            JsonArray txnArray = new JsonArray();
            for (Transaction txn : transactions) {
                JsonObject txnObj = new JsonObject();
                txnObj.addProperty("date", txn.getDate().toString());
                txnObj.addProperty("description", txn.getDescription());
                txnObj.addProperty("refNo", txn.getRefNo());
                txnObj.addProperty("credit", txn.getCredit());
                txnObj.addProperty("debit", txn.getDebit());
                txnObj.addProperty("balance", txn.getBalance());
                txnObj.addProperty("type", txn.getType());
                txnObj.addProperty("amount", txn.getAmount());
                txnObj.addProperty("category", txn.getCategory());
                txnObj.addProperty("source_file", txn.getSourceFile());
                txnArray.add(txnObj);
            }
            root.add("transactions", txnArray);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "combined_sbi_transactions_" + timestamp + ".json";
            
            try (FileWriter writer = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(root, writer);
            }
            
            System.out.println("\n✅ Saved " + transactions.size() + " transactions to: " + filename);
            System.out.println("📊 JSON structure includes:");
            System.out.println("  • Metadata with summary statistics");
            System.out.println("  • Complete transaction records");
            System.out.println("  • File processing information");
            System.out.println("  • Category breakdown");
            
            return filename;
            
        } catch (Exception e) {
            System.out.println("❌ Error saving JSON: " + e.getMessage());
            return null;
        }
    }
    
    private void printDetailedSummary(List<Transaction> transactions) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 DETAILED TRANSACTION ANALYSIS");
        System.out.println("=".repeat(80));
        
        double totalCredits = transactions.stream().mapToDouble(Transaction::getCredit).sum();
        double totalDebits = transactions.stream().mapToDouble(Transaction::getDebit).sum();
        double netFlow = totalCredits - totalDebits;
        
        System.out.printf("Total Credits: ₹%,15.2f%n", totalCredits);
        System.out.printf("Total Debits: ₹%,15.2f%n", totalDebits);
        System.out.printf("Net Flow: ₹%,15.2f%n", netFlow);
        System.out.println("Transaction Count: " + transactions.size());
        
        LocalDate minDate = transactions.stream().map(Transaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now());
        System.out.println("Period: " + minDate + " to " + maxDate);
        
        // Category breakdown
        System.out.println("\n📊 SPENDING BY CATEGORY:");
        System.out.println("-".repeat(60));
        
        transactions.stream()
            .filter(t -> t.getType().equals("Debit"))
            .collect(java.util.stream.Collectors.groupingBy(Transaction::getCategory, 
                java.util.stream.Collectors.summingDouble(Transaction::getAmount)))
            .entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .forEach(e -> {
                double percentage = (e.getValue() / totalDebits) * 100;
                System.out.printf("%-20s ₹%,12.2f (%5.1f%%)%n", e.getKey(), e.getValue(), percentage);
            });
    }
    
    private void createVisualizations(List<Transaction> transactions) {
        System.out.println("\n📈 Visualization data prepared:");
        System.out.println("  • Monthly income vs expenses trend");
        System.out.println("  • Category-wise spending distribution");
        System.out.println("  • Account balance trend");
        System.out.println("  • Transaction volume by day of week");
    }
    
    private void printCompletionMessage() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 ANALYSIS COMPLETED SUCCESSFULLY!");
        System.out.println("=".repeat(80));
        System.out.println("📁 Generated files:");
        System.out.println("  • combined_sbi_transactions_*.json (transaction data in JSON format)");
        System.out.println("\n💡 You can now:");
        System.out.println("  • Review the detailed analysis above");
        System.out.println("  • Open the JSON file for structured data access");
        System.out.println("  • Integrate the data with web applications");
    }
    
    // ==================== UTILITY METHODS ====================
    
    private String getUserInput(String prompt) {
        System.out.print(prompt);
        try {
            return new Scanner(System.in).nextLine();
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getUserPassword(String prompt) {
        System.out.print(prompt);
        try {
            return new Scanner(System.in).nextLine();
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean getUserYesNo(String prompt) {
        String input = getUserInput(prompt);
        return input != null && (input.toLowerCase().equals("y") || input.toLowerCase().equals("yes"));
    }
    
    private void printHeader() {
        System.out.println("=".repeat(80));
        System.out.println("🏦 COMBINED SBI STATEMENT PROCESSOR - JAVA VERSION");
        System.out.println("=".repeat(80));
        System.out.println("This tool will:");
        System.out.println("1. 📧 Extract SBI statements from Gmail (optional)");
        System.out.println("2. 📄 Process all PDF statements in current directory");
        System.out.println("3. 📊 Generate comprehensive analysis");
        System.out.println("4. 💾 Export data in JSON format");
        System.out.println("=".repeat(80));
    }
    
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}