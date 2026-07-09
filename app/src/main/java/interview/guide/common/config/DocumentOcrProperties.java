package interview.guide.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档 OCR 配置：本地 Tesseract 优先，失败时回退阿里云 DashScope OCR。
 */
@Component
@ConfigurationProperties(prefix = "app.document.ocr")
public class DocumentOcrProperties {

    private boolean enabled = true;
    private int minTextLength = 20;
    private int maxPages = 15;
    private int renderDpi = 200;
    private LocalOcr local = new LocalOcr();
    private AliyunOcr aliyun = new AliyunOcr();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinTextLength() {
        return minTextLength;
    }

    public void setMinTextLength(int minTextLength) {
        this.minTextLength = minTextLength;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getRenderDpi() {
        return renderDpi;
    }

    public void setRenderDpi(int renderDpi) {
        this.renderDpi = renderDpi;
    }

    public LocalOcr getLocal() {
        return local;
    }

    public void setLocal(LocalOcr local) {
        this.local = local;
    }

    public AliyunOcr getAliyun() {
        return aliyun;
    }

    public void setAliyun(AliyunOcr aliyun) {
        this.aliyun = aliyun;
    }

    public static class LocalOcr {
        private boolean enabled = true;
        private String language = "chi_sim+eng";
        private String datapath = "";
        private String tesseractPath = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getDatapath() {
            return datapath;
        }

        public void setDatapath(String datapath) {
            this.datapath = datapath;
        }

        public String getTesseractPath() {
            return tesseractPath;
        }

        public void setTesseractPath(String tesseractPath) {
            this.tesseractPath = tesseractPath;
        }
    }

    public static class AliyunOcr {
        private boolean enabled = true;
        private String apiKey = "";
        private String model = "qwen-vl-ocr-latest";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
