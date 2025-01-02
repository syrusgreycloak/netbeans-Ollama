/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import javax.swing.*;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

public class HyperlinkDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the frame
            JFrame frame = new JFrame("NetBeans Source Code Opener");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 200);

            // Create a JLabel with a hyperlink
            JLabel hyperlinkLabel = new JLabel("<html>Click <a href='#'>here</a> to open the source code.</html>");
            hyperlinkLabel.setHorizontalAlignment(SwingConstants.CENTER);

            // Add a mouse listener to handle the click
            hyperlinkLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        openSourceCode("src\\main\\java\\com\\stackleader\\netbeans\\chatgpt\\TaskManager.java", 10); // Replace with the actual file path and line number
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Failed to open the source code.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Add components to the frame
            frame.setLayout(new BorderLayout());
            frame.add(hyperlinkLabel, BorderLayout.CENTER);

            // Display the frame
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Opens the specified source file in NetBeans at a specific line.
     *
     * @param relativeFilePath The relative path to the source file in the project.
     * @param lineNumber       The line number to navigate to.
     */
    private static void openSourceCode(String relativeFilePath, int lineNumber) {
        try {
            // Resolve the file object using NetBeans APIs
            File file = new File(relativeFilePath);
            FileObject fileObject = FileUtil.toFileObject(file);
            System.out.println(file.getAbsolutePath());

            if (fileObject != null) {
                // Get the DataObject for the file
                DataObject dataObject = DataObject.find(fileObject);

                // Get the EditorCookie for the file
                EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);

                if (editorCookie != null) {
                    // Open the file in the editor
                    editorCookie.open();

                    // Navigate to the specified line
                    //editorCookie.getLineSet().getCurrent(lineNumber - 1).show(EditorCookie.ViewAction.FOCUS);
                } else {
                    System.err.println("EditorCookie not found for the file: " + relativeFilePath);
                }
            } else {
                System.err.println("FileObject not found for the file: " + relativeFilePath);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
