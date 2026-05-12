package com.sbi.processor;

import javax.mail.*;
import javax.mail.search.*;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;
import java.io.*;

public class GmailExtractor {
    private static final String IMAP_SERVER = "imap.gmail.com";
    private static final int IMAP_PORT = 993;
    private static final String SBI_SENDER = "projecttest24batch@gmail.com";
    private static final String SBI_SUBJECT = "Fwd: E-account statement for your SBI account(s).";
    
    public GmailExtractor() {
    }
    
    /**
     * Connect to Gmail using IMAP
     */
    public Store connectToGmail(String email, String appPassword) throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌟 STEP 1: GMAIL EXTRACTION");
        System.out.println("=".repeat(70));
        
        System.out.println("\n🔐 Gmail Login");
        System.out.println("=".repeat(40));
        
        Properties props = new Properties();
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1.2");
        props.put("mail.imap.socketFactory.port", String.valueOf(IMAP_PORT));
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        props.put("mail.imap.host", IMAP_SERVER);
        props.put("mail.imap.port", String.valueOf(IMAP_PORT));
        props.put("mail.imap.timeout", "10000");
        props.put("mail.imap.connectiontimeout", "10000");
        
        Session session = Session.getInstance(props, null);
        Store store = session.getStore("imaps");
        
        try {
            System.out.println("⌛ Connecting to IMAP server...");
            store.connect(IMAP_SERVER, email, appPassword);
            System.out.println("✅ Login successful!");
            System.out.println("📧 Connected as: " + email);
            return store;
        } catch (AuthenticationFailedException e) {
            System.out.println("❌ Login failed: Authentication error");
            System.out.println("\n🔑 Troubleshooting tips:");
            System.out.println("1. If you have 2-Factor Authentication enabled, use an App Password");
            System.out.println("2. If you don't have 2FA, enable 'Less secure app access' in Gmail settings");
            System.out.println("3. Make sure IMAP is enabled in Gmail settings");
            System.out.println("4. Check your email address and password");
            System.out.println("5. Wait a few minutes and try again (Gmail rate limiting)");
            throw new Exception("Gmail authentication failed", e);
        } catch (Exception e) {
            System.out.println("❌ Connection error: " + e.getMessage());
            throw new Exception("Failed to connect to Gmail IMAP server", e);
        }
    }
    
    /**
     * Get date range from user
     */
    public LocalDate[] getDateRange(Scanner scanner) {
        System.out.println("\n📅 Date Range Selection");
        System.out.println("=".repeat(40));
        System.out.println("Enter the date range for emails you want to process");
        System.out.println("Format: DD/MM/YYYY or DD-MM-YYYY");
        System.out.println("Examples: 01/01/2024, 15-06-2024");
        
        while (true) {
            try {
                System.out.print("\n📅 From date (DD/MM/YYYY): ");
                String fromDateStr = scanner.nextLine().trim();
                if (fromDateStr.isEmpty()) {
                    return null;
                }
                
                System.out.print("📅 To date (DD/MM/YYYY): ");
                String toDateStr = scanner.nextLine().trim();
                if (toDateStr.isEmpty()) {
                    return null;
                }
                
                LocalDate fromDate = parseDate(fromDateStr);
                LocalDate toDate = parseDate(toDateStr);
                
                if (fromDate == null || toDate == null) {
                    System.out.println("❌ Invalid date format. Please use DD/MM/YYYY format");
                    continue;
                }
                
                if (fromDate.isAfter(toDate)) {
                    System.out.println("❌ From date cannot be later than To date. Please try again.");
                    continue;
                }
                
                System.out.println("✅ Date range: " + fromDate + " to " + toDate);
                return new LocalDate[]{fromDate, toDate};
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Find SBI emails in date range
     */
    public List<Message> findSBIEmails(Folder inbox, LocalDate fromDate, LocalDate toDate) throws Exception {
        System.out.println("\n🔍 Searching for emails from " + fromDate + " to " + toDate + "...");
        
        try {
            Message[] allMessages = inbox.getMessages();
            List<Message> filtered = new ArrayList<>();
            
            System.out.println("📊 Searching through " + allMessages.length + " emails...");
            
            for (Message msg : allMessages) {
                try {
                    // Check sender
                    String from = msg.getFrom()[0].toString();
                    
                    // Check subject
                    String subject = msg.getSubject();
                    if (subject == null) continue;
                    
                    // Check date
                    Date receivedDate = msg.getReceivedDate();
                    if (receivedDate == null) {
                        // Try sent date
                        receivedDate = msg.getSentDate();
                    }
                    
                    if (receivedDate == null) continue;
                    
                    LocalDate emailDate = receivedDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                    
                    // Filter by date range and subject
                    if (!emailDate.isBefore(fromDate) && !emailDate.isAfter(toDate)) {
                        if (subject.contains("Fwd:") && (subject.contains("statement") || subject.contains("E-account"))) {
                            filtered.add(msg);
                            System.out.println("  ✅ Found: " + emailDate + " - " + subject);
                        }
                    }
                } catch (Exception e) {
                    // Skip this email and continue
                    continue;
                }
            }
            
            System.out.println("\n📊 Found " + filtered.size() + " matching email(s) in the date range");
            return filtered;
            
        } catch (Exception e) {
            System.out.println("❌ Error searching emails: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Extract PDF attachment from email
     */
    public byte[] extractPDFAttachment(Message msg) throws Exception {
        try {
            Object content = msg.getContent();
            
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                return extractFromMultipart(multipart);
            } else if (content instanceof String) {
                // No attachment
                return null;
            }
            
            return null;
            
        } catch (Exception e) {
            System.out.println("  ⚠️ Error extracting attachment: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Recursively extract PDF from multipart message
     */
    private byte[] extractFromMultipart(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            
            String disposition = part.getDisposition();
            String filename = part.getFileName();
            
            // Check if this is an attachment with PDF
            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                    InputStream is = part.getInputStream();
                    return readAllBytes(is);
                }
            }
            
            // Check content type
            String contentType = part.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/pdf")) {
                if (filename == null) {
                    filename = "statement.pdf";
                }
                InputStream is = part.getInputStream();
                return readAllBytes(is);
            }
            
            // Recursively check nested parts
            if (part.getContent() instanceof Multipart) {
                byte[] result = extractFromMultipart((Multipart) part.getContent());
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Read all bytes from input stream
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        return buffer.toByteArray();
    }
    
    /**
     * Save extracted PDFs to disk
     */
    public int savePDFsLocally(List<Message> emails) throws Exception {
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("\n📎 Extracting PDF attachments from " + emails.size() + " email(s)...");
        System.out.println("=".repeat(60));
        
        for (int i = 0; i < emails.size(); i++) {
            Message msg = emails.get(i);
            
            try {
                System.out.println("\n📄 Processing email " + (i + 1) + "/" + emails.size());
                
                // Get email date
                Date receivedDate = msg.getReceivedDate();
                if (receivedDate == null) {
                    receivedDate = msg.getSentDate();
                }
                
                if (receivedDate == null) {
                    System.out.println("  ⚠️ Email has no date, skipping");
                    failCount++;
                    continue;
                }
                
                LocalDateTime emailDateTime = receivedDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                
                System.out.println("  📅 Email date: " + emailDateTime);
                System.out.println("  📧 Subject: " + msg.getSubject());
                
                // Extract PDF
                byte[] pdfData = extractPDFAttachment(msg);
                
                if (pdfData != null && pdfData.length > 0) {
                    // Create filename with date
                    String dateStr = emailDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String timeStr = emailDateTime.format(DateTimeFormatter.ofPattern("HHmm"));
                    String filename = "SBI_Statement_" + dateStr + "_" + timeStr + ".pdf";
                    
                    // Ensure unique filename
                    int counter = 1;
                    String baseFilename = filename;
                    while (new File(filename).exists()) {
                        String name = baseFilename.substring(0, baseFilename.lastIndexOf("."));
                        filename = name + "_v" + counter + ".pdf";
                        counter++;
                    }
                    
                    // Save file
                    try (FileOutputStream fos = new FileOutputStream(filename)) {
                        fos.write(pdfData);
                    }
                    
                    System.out.println("  ✅ Saved as: " + filename + " (" + (pdfData.length / 1024) + " KB)");
                    successCount++;
                } else {
                    System.out.println("  ⚠️ No PDF attachment found in this email");
                    failCount++;
                }
                
            } catch (Exception e) {
                System.out.println("  ❌ Error processing email: " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 GMAIL EXTRACTION SUMMARY:");
        System.out.println("✅ Successfully extracted: " + successCount + " PDF(s)");
        System.out.println("❌ Failed: " + failCount + " email(s)");
        System.out.println("📁 Files saved in: " + System.getProperty("user.dir"));
        
        return successCount;
    }
    
    /**
     * Parse date string in DD/MM/YYYY or DD-MM-YYYY format
     */
    private LocalDate parseDate(String dateStr) {
        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy"};
        
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                continue;
            }
        }
        
        return null;
    }
    
    /**
     * Display Gmail setup instructions
     */
    public static void displayGmailSetupInstructions() {
        System.out.println("\n🔑 GMAIL SETUP INSTRUCTIONS:");
        System.out.println("=".repeat(50));
        System.out.println("\n📝 If you have 2-Factor Authentication (RECOMMENDED):");
        System.out.println("  1. Go to: https://myaccount.google.com/apppasswords");
        System.out.println("  2. Select 'Mail' and 'Windows Computer' (or your device)");
        System.out.println("  3. Google will generate a 16-character app password");
        System.out.println("  4. Use THIS password in the application (not your Gmail password)");
        System.out.println("  5. Example: 'abcd efgh ijkl mnop' (copy without spaces)");
        
        System.out.println("\n📝 If you DON'T have 2-Factor Authentication:");
        System.out.println("  1. Go to: https://myaccount.google.com/security");
        System.out.println("  2. Under 'Less secure app access', enable it");
        System.out.println("  3. Use your regular Gmail password");
        
        System.out.println("\n⚠️ IMPORTANT:");
        System.out.println("  • IMAP must be enabled in Gmail settings");
        System.out.println("  • 2FA + App Password is MORE secure");
        System.out.println("  • Never share your password or app password");
        System.out.println("=".repeat(50));
    }
}