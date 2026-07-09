package interview.guide.infrastructure.file.ocr;

import interview.guide.common.config.DocumentOcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地 Tesseract OCR，优先用于扫描版 PDF。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalTesseractOcrService {

    private final DocumentOcrProperties properties;
    private final PdfPageImageRenderer pdfPageImageRenderer;

    private volatile ITesseract tesseract;
    private volatile boolean unavailableLogged;

    public String extractFromPdf(byte[] pdfBytes) {
        DocumentOcrProperties.LocalOcr local = properties.getLocal();
        if (!properties.isEnabled() || !local.isEnabled()) {
            return "";
        }

        ITesseract engine = getOrCreateTesseract();
        if (engine == null) {
            return "";
        }

        try {
            List<BufferedImage> pages = pdfPageImageRenderer.renderPages(
                pdfBytes,
                properties.getMaxPages(),
                properties.getRenderDpi()
            );
            List<String> pageTexts = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                String pageText = engine.doOCR(pages.get(i));
                if (StringUtils.hasText(pageText)) {
                    pageTexts.add(pageText.trim());
                }
                log.debug("Local OCR page {} extracted {} chars", i + 1, pageText != null ? pageText.length() : 0);
            }
            return String.join("\n\n", pageTexts);
        } catch (IOException e) {
            log.warn("Local OCR failed to render PDF pages: {}", e.getMessage());
            return "";
        } catch (TesseractException e) {
            log.warn("Local OCR recognition failed: {}", e.getMessage());
            return "";
        }
    }

    private ITesseract getOrCreateTesseract() {
        if (tesseract != null) {
            return tesseract;
        }
        synchronized (this) {
            if (tesseract != null) {
                return tesseract;
            }
            ITesseract engine = createTesseract();
            if (engine == null && !unavailableLogged) {
                unavailableLogged = true;
                log.warn("Local Tesseract OCR unavailable. Install Tesseract and chi_sim language pack, "
                    + "or set APP_OCR_TESSDATA_PATH / APP_OCR_TESSERACT_PATH.");
            }
            tesseract = engine;
            return tesseract;
        }
    }

    private ITesseract createTesseract() {
        DocumentOcrProperties.LocalOcr local = properties.getLocal();
        String datapath = resolveDatapath(local.getDatapath());
        if (!StringUtils.hasText(datapath)) {
            return null;
        }

        Path chiSim = Path.of(datapath, "chi_sim.traineddata");
        Path eng = Path.of(datapath, "eng.traineddata");
        if (!Files.exists(chiSim) && !Files.exists(eng)) {
            log.debug("No OCR language data found under {}", datapath);
            return null;
        }

        Tesseract engine = new Tesseract();
        engine.setDatapath(datapath);
        engine.setLanguage(local.getLanguage());
        engine.setPageSegMode(1);
        log.info("Local Tesseract OCR initialized: datapath={}, language={}", datapath, local.getLanguage());
        return engine;
    }

    private String resolveDatapath(String configuredPath) {
        if (StringUtils.hasText(configuredPath)) {
            Path path = Path.of(configuredPath);
            return Files.isDirectory(path) ? path.toString() : null;
        }

        String envPath = System.getenv("TESSDATA_PREFIX");
        if (StringUtils.hasText(envPath)) {
            Path path = Path.of(envPath);
            if (Files.isDirectory(path)) {
                return path.toString();
            }
        }

        List<Path> candidates = defaultDatapathCandidates();
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("chi_sim.traineddata"))) {
                return candidate.toString();
            }
        }
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("eng.traineddata"))) {
                return candidate.toString();
            }
        }
        return null;
    }

    private List<Path> defaultDatapathCandidates() {
        List<Path> candidates = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            candidates.add(Path.of("C:/Program Files/Tesseract-OCR/tessdata"));
            candidates.add(Path.of("C:/Program Files (x86)/Tesseract-OCR/tessdata"));
        } else if (os.contains("mac")) {
            candidates.add(Path.of("/opt/homebrew/share/tessdata"));
            candidates.add(Path.of("/usr/local/share/tessdata"));
        } else {
            candidates.add(Path.of("/usr/share/tesseract-ocr/4.00/tessdata"));
            candidates.add(Path.of("/usr/share/tessdata"));
        }
        return candidates;
    }
}
