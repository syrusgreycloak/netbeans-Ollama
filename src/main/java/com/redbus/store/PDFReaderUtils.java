/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.store;

/**
 *
 * @author manoj.kumar
 */
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFReaderUtils {
     public static void main(String[] args) {
        File selectedFile = PDFReaderUtils.selectPDFFile();
        if (selectedFile != null) {
            String[] pdfParagraphs = PDFReaderUtils.extractTextAsParagraphs(selectedFile);
            if (pdfParagraphs != null) {
                System.out.println("Extracted Paragraphs:");
                for (int i = 0; i < pdfParagraphs.length; i++) {
                    System.out.println("Paragraph " + (i + 1) + ": " + pdfParagraphs[i]);
                }
            } else {
                System.out.println("Failed to extract text from the selected PDF.");
            }
        } else {
            System.out.println("No file selected.");
        }
        
        
        if (selectedFile != null) {
            String[] pdfChunks = PDFReaderUtils.extractTextInChunks(selectedFile, 256);
            if (pdfChunks != null) {
                System.out.println("Extracted Chunks:");
                for (int i = 0; i < pdfChunks.length; i++) {
                    System.out.println("Chunk " + (i + 1) + ": " + pdfChunks[i]);
                }
            } else {
                System.out.println("Failed to extract text from the selected PDF.");
            }
        } else {
            System.out.println("No file selected.");
        }
    }
    
    
     /**
     * Displays a file chooser dialog to select a PDF file.
     *
     * @return the selected PDF file, or null if no file is selected.
     */
    public static File selectPDFFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a PDF File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        int userChoice = fileChooser.showOpenDialog(null);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Extracts text from the given PDF file.
     *
     * @param pdfFile the PDF file to read.
     * @return the extracted text, or null if an error occurs.
     */
    public static String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (!document.isEncrypted()) {
                PDFTextStripper textStripper = new PDFTextStripper();
                return textStripper.getText(document);
            } else {
                System.out.println("The selected PDF is encrypted and cannot be read.");
            }
        } catch (IOException e) {
            System.err.println("Error loading or reading the PDF: " + e.getMessage());
        }
        return null;
    }
    
    
        /**
     * Extracts text from the given PDF file and splits it into an array of paragraphs.
     *
     * @param pdfFile the PDF file to read.
     * @return an array of strings, each representing a paragraph, or null if an error occurs.
     */
    public static String[] extractTextAsParagraphs(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (!document.isEncrypted()) {
                PDFTextStripper textStripper = new PDFTextStripper();
                String extractedText = textStripper.getText(document);
                // Split the text by paragraphs (new lines)
                return extractedText.split("(?m)(?:\\r?\\n){2,}");
            } else {
                System.out.println("The selected PDF is encrypted and cannot be read.");
            }
        } catch (IOException e) {
            System.err.println("Error loading or reading the PDF: " + e.getMessage());
        }
        return null;
    }
    
     /**
     * Extracts text from the given PDF file and splits it into chunks of the specified size.
     *
     * @param pdfFile the PDF file to read.
     * @param chunkSize the size of each text chunk.
     * @return an array of strings, each representing a chunk of text, or null if an error occurs.
     */
    public static String[] extractTextInChunks(File pdfFile, int chunkSize) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (!document.isEncrypted()) {
                PDFTextStripper textStripper = new PDFTextStripper();
                String extractedText = textStripper.getText(document);
                return splitTextIntoChunks(extractedText, chunkSize);
            } else {
                System.out.println("The selected PDF is encrypted and cannot be read.");
            }
        } catch (IOException e) {
            System.err.println("Error loading or reading the PDF: " + e.getMessage());
        }
        return null;
    }

    /**
 * Splits the given text into chunks of the specified size without clipping words.
 *
 * @param text the text to split.
 * @param chunkSize the approximate size of each chunk.
 * @return an array of strings, each representing a chunk of text.
 */
private static String[] splitTextIntoChunks(String text, int chunkSize) {
    List<String> chunks = new ArrayList<>();
    int length = text.length();
    int start = 0;

    while (start < length) {
        int end = Math.min(start + chunkSize, length);

        // Ensure we don't break a word in half
        if (end < length && !Character.isWhitespace(text.charAt(end))) {
            int lastSpace = text.lastIndexOf(' ', end);
            if (lastSpace > start) {
                end = lastSpace;
            }
        }

        chunks.add(text.substring(start, end).trim());
        start = end; // Start the next chunk after the current one
    }

    return chunks.toArray(new String[0]);
}

}
