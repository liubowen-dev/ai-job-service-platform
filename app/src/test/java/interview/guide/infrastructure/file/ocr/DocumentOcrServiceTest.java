package interview.guide.infrastructure.file.ocr;

import interview.guide.common.config.DocumentOcrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("文档 OCR 服务测试")
class DocumentOcrServiceTest {

    @Mock
    private LocalTesseractOcrService localTesseractOcrService;

    @Mock
    private AliyunDashScopeOcrService aliyunDashScopeOcrService;

    private DocumentOcrProperties properties;
    private DocumentOcrService documentOcrService;

    @BeforeEach
    void setUp() {
        properties = new DocumentOcrProperties();
        properties.setEnabled(true);
        properties.setMinTextLength(20);
        documentOcrService = new DocumentOcrService(properties, localTesseractOcrService, aliyunDashScopeOcrService);
    }

    @Test
    @DisplayName("Tika 文本足够时不触发 OCR")
    void shouldSkipOcrWhenTikaTextIsEnough() {
        byte[] pdfBytes = new byte[] {1, 2, 3};
        String tikaText = "这是一份足够长的简历文本内容，用于跳过 OCR 兜底逻辑。";

        String result = documentOcrService.tryExtractText(pdfBytes, "resume.pdf", tikaText);

        assertEquals("", result);
        verify(localTesseractOcrService, never()).extractFromPdf(pdfBytes);
        verify(aliyunDashScopeOcrService, never()).extractFromPdf(pdfBytes);
    }

    @Test
    @DisplayName("本地 OCR 成功时不调用阿里云")
    void shouldUseLocalOcrFirst() {
        byte[] pdfBytes = new byte[] {1, 2, 3};
        when(localTesseractOcrService.extractFromPdf(pdfBytes)).thenReturn("本地 OCR 识别出的简历内容，长度足够。");

        String result = documentOcrService.tryExtractText(pdfBytes, "resume.pdf", "");

        assertEquals("本地 OCR 识别出的简历内容，长度足够。", result);
        verify(aliyunDashScopeOcrService, never()).extractFromPdf(pdfBytes);
    }

    @Test
    @DisplayName("本地 OCR 失败时回退阿里云")
    void shouldFallbackToAliyunWhenLocalFails() {
        byte[] pdfBytes = new byte[] {1, 2, 3};
        when(localTesseractOcrService.extractFromPdf(pdfBytes)).thenReturn("");
        when(aliyunDashScopeOcrService.extractFromPdf(pdfBytes)).thenReturn("阿里云 OCR 识别出的简历内容，长度足够。");

        String result = documentOcrService.tryExtractText(pdfBytes, "resume.pdf", " ");

        assertEquals("阿里云 OCR 识别出的简历内容，长度足够。", result);
    }
}
