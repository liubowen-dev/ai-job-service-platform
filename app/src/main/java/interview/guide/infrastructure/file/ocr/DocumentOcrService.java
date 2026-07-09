package interview.guide.infrastructure.file.ocr;

import interview.guide.common.config.DocumentOcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文档 OCR 编排：Tika 提取不足时，先本地 Tesseract，再阿里云 DashScope OCR。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentOcrService {

    private final DocumentOcrProperties properties;
    private final LocalTesseractOcrService localTesseractOcrService;
    private final AliyunDashScopeOcrService aliyunDashScopeOcrService;

    /**
     * 当 Tika 提取文本不足时，尝试 OCR 兜底。
     *
     * @param fileBytes  原始文件字节
     * @param fileName   原始文件名
     * @param tikaText   Tika 已提取文本
     * @return OCR 文本；无需 OCR 或 OCR 失败时返回空字符串
     */
    public String tryExtractText(byte[] fileBytes, String fileName, String tikaText) {
        if (!shouldTryOcr(fileName, tikaText)) {
            return "";
        }

        log.info("Tika extracted insufficient text from {}, starting OCR fallback", fileName);

        String localText = localTesseractOcrService.extractFromPdf(fileBytes);
        if (hasEnoughText(localText)) {
            log.info("Local Tesseract OCR succeeded for {}", fileName);
            return localText;
        }

        String cloudText = aliyunDashScopeOcrService.extractFromPdf(fileBytes);
        if (hasEnoughText(cloudText)) {
            log.info("Aliyun DashScope OCR succeeded for {}", fileName);
            return cloudText;
        }

        log.warn("OCR fallback failed for {}", fileName);
        return "";
    }

    private boolean shouldTryOcr(String fileName, String tikaText) {
        if (!properties.isEnabled() || !isPdf(fileName)) {
            return false;
        }
        return !hasEnoughText(tikaText);
    }

    private boolean hasEnoughText(String text) {
        return StringUtils.hasText(text) && text.trim().length() >= properties.getMinTextLength();
    }

    private boolean isPdf(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }
}
