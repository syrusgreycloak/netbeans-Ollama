/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.regex.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import org.json.JSONException;
/**
 *
 * @author manoj.kumar
 */
public class CUAHelper {
    
        // Define the pattern for finding a JSON array within a code block.
    // Using a static final field for efficiency.
    // Regex Breakdown:
    // (?s)        - Enable DOTALL mode ('.' matches newline characters)
    // ```         - Match the literal starting backticks
    // (?:\\w*\\s*)? - Optional non-capturing group:
    //                 \\w*    - Match zero or more word characters (for language hint like 'json', 'java', etc.)
    //                 \\s*    - Match zero or more whitespace after the language hint
    //                 (?:...)? - Make the whole language hint + space part optional
    // (           - Start of capturing group 1 (this is what we want to extract)
    // \\[        - Match the literal opening bracket of the JSON array
    // .*?         - Match any character (including newlines due to (?s)) zero or more times, NON-GREEDILY
    //             - The non-greedy '?' is crucial to stop at the first closing ']' associated with the block.
    // \\]         - Match the literal closing bracket of the JSON array
    // )           - End of capturing group 1
    // \\s*        - Match zero or more whitespace before the closing backticks
    // ```         - Match the literal closing backticks
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "(?s)```(?:\\w*\\s*)?(\\[.*?\\])\\s*```"
    );
    
    /**
     * Extracts and parses a JSON array string found within a Markdown code
     * block (e.g., ```json [...] ``` or ``` [...] ```) from the input text.
     *
     * @param inputText The string potentially containing the JSON array within
     * a code block.
     * @return A org.json.JSONArray parsed from the extracted string.
     * @throws IllegalArgumentException if no matching code block containing a
     * JSON array is found, or if the extracted content is not valid JSON.
     */
    public static JSONArray extractJsonArray(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty.");
        }

        Matcher matcher = JSON_ARRAY_PATTERN.matcher(inputText);

        // matcher.find() attempts to find the pattern anywhere in the string
        if (matcher.find()) {
            // matcher.group(1) gets the content of the first capturing group,
            // which is the JSON array string between the brackets.
            String jsonArrayText = matcher.group(1);

            try {
                // Attempt to parse the extracted string as a JSONArray
                return new JSONArray(jsonArrayText);
            } catch (JSONException e) {
                // If parsing fails, it means the content inside the block
                // looked like an array pattern but was not valid JSON.
                // throw new IllegalArgumentException( "Found code block matching array pattern, but content is not valid JSON: " + jsonArrayText, e);
            }

        } else {
            // If matcher.find() returns false, the pattern was not found.
            // throw new IllegalArgumentException("No ```...``` block containing a JSON array pattern found in the input.");
        }
        return null;
    }

     public static void displayImageWithBoxes(BufferedImage originalImage, Component parent, String label, String jsonBoxes) throws IOException {
        // Load original image
        //BufferedImage originalImage = ImageIO.read(imageFile);

        // Draw bounding boxes on a copy of the image
        BufferedImage annotatedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2d = annotatedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);

        // Optional style
        g2d.setStroke(new BasicStroke(3));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        // Parse and draw bounding boxes
        JSONArray boxes = new JSONArray(jsonBoxes);
        for (int i = 0; i < boxes.length(); i++) {
            JSONObject obj = boxes.getJSONObject(i);
            JSONArray bbox = obj.getJSONArray("bbox_2d");
            String boxLabel = obj.optString("label", "Box");

            int x1 = bbox.getInt(0);
            int y1 = bbox.getInt(1);
            int x2 = bbox.getInt(2);
            int y2 = bbox.getInt(3);

            int width = x2 - x1;
            int height = y2 - y1;

            // Draw rectangle and label
            g2d.setColor(Color.RED);
            g2d.drawRect(x1, y1, width, height);
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(x1, y1 - 18, g2d.getFontMetrics().stringWidth(boxLabel) + 6, 18);
            g2d.setColor(Color.BLACK);
            g2d.drawString(boxLabel, x1 + 3, y1 - 3);
        }
        g2d.dispose();

        // Resize for display
        //Image scaledImage = scaleImage(annotatedImage, 1024, 800);

        // GUI setup
        JLabel imageLabel = new JLabel(new ImageIcon(originalImage));
        JTextArea labelArea = new JTextArea( label, 5, 80);
        labelArea.setFont(new Font("Arial", Font.PLAIN, 16));
        labelArea.setLineWrap(true);
        labelArea.setWrapStyleWord(true);
        labelArea.setEditable(false);
        labelArea.setBackground(UIManager.getColor("Panel.background"));

        JDialog dialog = new JDialog();
        dialog.setTitle("Marked Image");
        dialog.setLayout(new BorderLayout());
        dialog.add(imageLabel, BorderLayout.CENTER);
        dialog.add(new JScrollPane(labelArea), BorderLayout.SOUTH);
        dialog.setSize(1024, 800);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
     
    public static Image scaleImage(BufferedImage src, int maxWidth, int maxHeight) {
    int width = src.getWidth();
    int height = src.getHeight();
    double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
    int newWidth = (int) (width * scale);
    int newHeight = (int) (height * scale);
    return src.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
}
   /**
     * Converts an image file to a Base64-encoded string, scaling it to 800x600 or 600x800 while maintaining aspect ratio.
     *
     * @param imagePath Path to the image file.
     * @return Base64-encoded string of the scaled image.
     * @throws IOException If an error occurs during file reading or image processing.
     */
    public static String convertImageToBase64(String imagePath) throws IOException {
        try {
            BufferedImage originalImage = ImageIO.read(new File(imagePath));
            if (originalImage == null) {
                throw new IOException("Could not read image file: " + imagePath);
            }

            BufferedImage scaledImage = scaleImageToFit(originalImage);

            return convertImageToBase64(scaledImage);

        } catch (IOException e) {
            throw new IOException("Error processing image: " + e.getMessage(), e);
        }
    }


    /**
     * Scales a BufferedImage to fit within 800x600 or 600x800, maintaining aspect ratio.
     *
     * @param originalImage The original BufferedImage.
     * @return The scaled BufferedImage.
     */
    public static BufferedImage scaleImageToFit(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int maxWidth = 800;
        int maxHeight = 600;
        int newWidth, newHeight;

        // Determine if the image is wider or taller
        if ((double) originalWidth / originalHeight > (double) maxWidth / maxHeight) {
            // Wider image: Scale to fit width
            newWidth = maxWidth;
            newHeight = (int) (originalHeight * ((double) maxWidth / originalWidth));
        } else {
            // Taller or square image: Scale to fit height
            newHeight = maxHeight;
            newWidth = (int) (originalWidth * ((double) maxHeight / originalHeight));
        }

        // Create a new BufferedImage with the calculated dimensions
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }


    /**
     * Converts a BufferedImage to a Base64-encoded string.
     *
     * @param image The BufferedImage to convert.
     * @return Base64-encoded string of the image.
     * @throws IOException If an error occurs during image processing.
     */
    public static String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream); // You can change "png" to "jpg" or other supported formats
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
