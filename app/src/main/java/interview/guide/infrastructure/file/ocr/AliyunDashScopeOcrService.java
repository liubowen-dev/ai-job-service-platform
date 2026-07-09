package interview.guide.infrastructure.file.ocr;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.OcrOptions;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import interview.guide.common.config.DocumentOcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼 Qwen-VL-OCR，本地 OCR 失败时的云端兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunDashScopeOcrService {

    private static final int MAX_PIXELS = 8_388_608;
    private static final int MIN_PIXELS = 3072;

    private final DocumentOcrProperties properties;
    private final PdfPageImageRenderer pdfPageImageRenderer;

    public String extractFromPdf(byte[] pdfBytes) {
        DocumentOcrProperties.AliyunOcr aliyun = properties.getAliyun();
        if (!properties.isEnabled() || !aliyun.isEnabled() || !StringUtils.hasText(aliyun.getApiKey())) {
            return "";
        }

        try {
            List<BufferedImage> pages = pdfPageImageRenderer.renderPages(
                pdfBytes,
                properties.getMaxPages(),
                properties.getRenderDpi()
            );
            List<String> pageTexts = new ArrayList<>();
            MultiModalConversation conversation = new MultiModalConversation();
            for (int i = 0; i < pages.size(); i++) {
                String pageText = recognizePage(conversation, aliyun, pages.get(i));
                if (StringUtils.hasText(pageText)) {
                    pageTexts.add(pageText.trim());
                }
                log.debug("Aliyun OCR page {} extracted {} chars", i + 1, pageText != null ? pageText.length() : 0);
            }
            return String.join("\n\n", pageTexts);
        } catch (IOException e) {
            log.warn("Aliyun OCR failed to render PDF pages: {}", e.getMessage());
            return "";
        }
    }

    private String recognizePage(
        MultiModalConversation conversation,
        DocumentOcrProperties.AliyunOcr aliyun,
        BufferedImage image
    ) {
        try {
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("image", toDataUri(image));
            imageContent.put("max_pixels", MAX_PIXELS);
            imageContent.put("min_pixels", MIN_PIXELS);
            imageContent.put("enable_rotate", true);

            MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(imageContent))
                .build();

            OcrOptions ocrOptions = OcrOptions.builder()
                .task(OcrOptions.Task.TEXT_RECOGNITION)
                .build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(aliyun.getApiKey())
                .model(aliyun.getModel())
                .message(userMessage)
                .ocrOptions(ocrOptions)
                .build();

            MultiModalConversationResult result = conversation.call(param);
            return extractText(result);
        } catch (IOException e) {
            log.warn("Aliyun OCR failed to encode page image: {}", e.getMessage());
            return "";
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            log.warn("Aliyun OCR page recognition failed: {}", e.getMessage());
            return "";
        }
    }

    private String extractText(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null || result.getOutput().getChoices() == null) {
            return "";
        }
        if (result.getOutput().getChoices().isEmpty()) {
            return "";
        }
        MultiModalMessage message = result.getOutput().getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
            return "";
        }
        Object first = message.getContent().get(0);
        if (first instanceof Map<?, ?> contentMap) {
            Object text = contentMap.get("text");
            return text != null ? text.toString() : "";
        }
        return first != null ? first.toString() : "";
    }

    private String toDataUri(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + base64;
    }
}
