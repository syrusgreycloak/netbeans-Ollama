/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.store;

/**
 *
 * @author manoj.kumar
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

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
        
        
      String outputDirectory = "output"; // Directory to save images

        // Create the output directory if it doesn't exist
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        try (PDDocument document = PDDocument.load(selectedFile)) {
            // Create a PDFRenderer to render PDF pages
            PDFRenderer renderer = new PDFRenderer(document);

            // Loop through each page and save it as an image
            for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); pageNumber++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(pageNumber, 300); // 300 DPI for high-quality images

                // Create output file name
                String outputFilePath = String.format("%s/page_%d.png", outputDirectory, pageNumber + 1);

                // Save the image
                ImageIO.write(bufferedImage, "PNG", new File(outputFilePath));
                System.out.println("Extracted Page " + (pageNumber + 1) + " to " + outputFilePath);
            }

            System.out.println("All pages have been extracted successfully.");
        } catch (IOException e) {
            System.err.println("Error processing PDF file: " + e.getMessage());
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

    
     /**
     * Displays the selected image with a label below it in a new JDialog.
     *
     * @param imageFile the selected image file
     * @param parent    the parent component for the dialog
     */
    public  static void displayImageWithLabel(File imageFile, Component parent, String label) throws IOException {
        // Create a JDialog to display the image
        JDialog dialog = new JDialog();
        dialog.setTitle(imageFile.getName());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(1024, 800);

        // Load the image and scale it
        BufferedImage originalImage = ImageIO.read(imageFile);
        Image scaledImage = scaleImage(originalImage, 1024, 800);

        // Create an image label and a text label
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        JTextArea fileNameLabel = new JTextArea(imageFile.getName()+": "+label, 10, 200);
        fileNameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        fileNameLabel.setLineWrap(true);
        fileNameLabel.setWrapStyleWord(true);
        fileNameLabel.setEditable(false);
        fileNameLabel.setBackground(dialog.getBackground()); // Match background for a cleaner look

        // Add components to the dialog
        dialog.setLayout(new BorderLayout());
        dialog.add(imageLabel, BorderLayout.CENTER);

        JScrollPane labelScrollPane = new JScrollPane(fileNameLabel);
        dialog.add(labelScrollPane, BorderLayout.SOUTH);

        // Center the dialog relative to the parent component
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

       /**
     * Scales an image to fit within the given max width and height while maintaining aspect ratio.
     *
     * @param originalImage the original image to scale
     * @param maxWidth      the maximum width of the scaled image
     * @param maxHeight     the maximum height of the scaled image
     * @return the scaled image
     */
    private static Image scaleImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scaleFactor = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * scaleFactor);
        int newHeight = (int) (originalHeight * scaleFactor);

        return originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

}
