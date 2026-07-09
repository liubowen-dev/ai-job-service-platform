package interview.guide.infrastructure.file.ocr;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 PDF 页面渲染为图片，供 OCR 使用。
 */
@Slf4j
@Component
public class PdfPageImageRenderer {

    public List<BufferedImage> renderPages(byte[] pdfBytes, int maxPages, int dpi) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = Math.min(document.getNumberOfPages(), Math.max(maxPages, 1));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                images.add(renderer.renderImageWithDPI(pageIndex, dpi));
            }
            log.debug("Rendered {} PDF page(s) at {} DPI", pageCount, dpi);
        }
        return images;
    }
}
