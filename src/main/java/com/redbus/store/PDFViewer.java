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
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFViewer extends JFrame {
    private JPanel pdfPanel;
    private PDDocument document;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;

    public PDFViewer(String pdfFilePath) {
        setTitle("PDF Reader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        pdfPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (document != null) {
                    try {
                        BufferedImage pageImage = pdfRenderer.renderImage(currentPage, 2.0f); // Scale = 2.0 for better quality
                        g.drawImage(pageImage, 0, 0, this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        JScrollPane scrollPane = new JScrollPane(pdfPanel);
        add(scrollPane, BorderLayout.CENTER);

        JPanel navigationPanel = new JPanel();
        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        navigationPanel.add(previousButton);
        navigationPanel.add(nextButton);
        add(navigationPanel, BorderLayout.SOUTH);

        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                pdfPanel.repaint();
            }
        });

        nextButton.addActionListener(e -> {
            if (currentPage < document.getNumberOfPages() - 1) {
                currentPage++;
                pdfPanel.repaint();
            }
        });

        try {
            document =   PDDocument.load(new File(pdfFilePath));
            pdfRenderer = new PDFRenderer(document);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load PDF file.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PDFViewer("path/to/your/file.pdf"));
    }
}
