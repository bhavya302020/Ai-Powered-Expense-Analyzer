package com.sbi.processor;

import javax.mail.Message;

public class EmailWithPDF {
    private Message message;
    private byte[] pdfData;

    public EmailWithPDF(Message message, byte[] pdfData) {
        this.message = message;
        this.pdfData = pdfData;
    }

    public Message getMessage() {
        return message;
    }

    public byte[] getPdfData() {
        return pdfData;
    }
}