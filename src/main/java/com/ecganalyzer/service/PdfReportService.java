package com.ecganalyzer.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PdfReportService {

    private static final float MARGIN = 48f;
    private static final float TITLE_FONT_SIZE = 18f;
    private static final float BODY_FONT_SIZE = 10.5f;
    private static final float LINE_HEIGHT = 14f;
    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void exportAnalysisReport(File outputFile, String reportText, WritableImage chartImage) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file is required");
        }
        if (reportText == null || reportText.isBlank()) {
            throw new IllegalArgumentException("Report text is required");
        }

        try (PDDocument document = new PDDocument()) {
            PageWriter writer = new PageWriter(document);
            writer.writeTitle("ECG ANALYSIS REPORT");
            writer.writeLine("Generated: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            writer.writeLine("");

            String normalizedText = reportText
                    .replace("ECG ANALYSIS REPORT", "")
                    .replace("===================", "")
                    .strip();
            writer.writeWrappedText(normalizedText);

            if (chartImage != null) {
                writer.writeSectionGap();
                writer.writeLine("ECG chart snapshot");
                writer.writeImage(chartImage);
            }

            writer.closeCurrentStream();
            document.save(outputFile);
        }
    }

    private static final class PageWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PageWriter(PDDocument document) throws IOException {
            this.document = document;
            startNewPage();
        }

        private void startNewPage() throws IOException {
            closeCurrentStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void closeCurrentStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private void writeTitle(String title) throws IOException {
            ensureSpace(TITLE_FONT_SIZE + LINE_HEIGHT);
            contentStream.beginText();
            contentStream.setFont(TITLE_FONT, TITLE_FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(safeText(title));
            contentStream.endText();
            y -= TITLE_FONT_SIZE + LINE_HEIGHT;
        }

        private void writeWrappedText(String text) throws IOException {
            String[] paragraphs = text.split("\\R", -1);
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    writeLine("");
                    continue;
                }
                for (String line : wrap(paragraph, BODY_FONT, BODY_FONT_SIZE, getTextWidth())) {
                    writeLine(line);
                }
            }
        }

        private void writeLine(String line) throws IOException {
            ensureSpace(LINE_HEIGHT);
            contentStream.beginText();
            contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(safeText(line));
            contentStream.endText();
            y -= LINE_HEIGHT;
        }

        private void writeSectionGap() throws IOException {
            ensureSpace(LINE_HEIGHT * 2);
            y -= LINE_HEIGHT;
        }

        private void writeImage(WritableImage image) throws IOException {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

            float availableWidth = getTextWidth();
            float maxHeight = 260f;
            float scale = Math.min(availableWidth / pdImage.getWidth(), maxHeight / pdImage.getHeight());
            float imageWidth = pdImage.getWidth() * scale;
            float imageHeight = pdImage.getHeight() * scale;

            ensureSpace(imageHeight + LINE_HEIGHT);
            contentStream.drawImage(pdImage, MARGIN, y - imageHeight, imageWidth, imageHeight);
            y -= imageHeight + LINE_HEIGHT;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN) {
                startNewPage();
            }
        }

        private float getTextWidth() {
            return page.getMediaBox().getWidth() - 2 * MARGIN;
        }

        private static List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String safe = safeText(text);
            String[] words = safe.split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    if (textWidth(word, font, fontSize) <= maxWidth) {
                        line.append(word);
                    } else {
                        lines.addAll(splitLongWord(word, font, fontSize, maxWidth));
                    }
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }

        private static List<String> splitLongWord(String word, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> parts = new ArrayList<>();
            StringBuilder part = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                String candidate = part.toString() + word.charAt(i);
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    part.append(word.charAt(i));
                } else {
                    if (!part.isEmpty()) {
                        parts.add(part.toString());
                    }
                    part.setLength(0);
                    part.append(word.charAt(i));
                }
            }
            if (!part.isEmpty()) {
                parts.add(part.toString());
            }
            return parts;
        }

        private static float textWidth(String text, PDFont font, float fontSize) throws IOException {
            return font.getStringWidth(safeText(text)) / 1000f * fontSize;
        }

        private static String safeText(String text) {
            if (text == null) {
                return "";
            }
            String normalized = text
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("±", "+-")
                    .replace("≈", "~")
                    .replace("≤", "<=")
                    .replace("≥", ">=")
                    .replace("×", "x")
                    .replace("’", "'")
                    .replace("“", "\"")
                    .replace("”", "\"");

            StringBuilder result = new StringBuilder(normalized.length());
            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                if (c >= 32 && c <= 126) {
                    result.append(c);
                } else {
                    result.append('?');
                }
            }
            return result.toString();
        }
    }
}
