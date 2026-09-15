package br.lcn.ragkb.service;

import br.lcn.ragkb.exception.InvalidArticleContentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleSanitizationService {

    private static final String MEDIA_PREFIX = "/api/media/articles/";

    /**
     * Sanitizes rich-text HTML from the editor before persistence.
     * - Whitelist-based: only known-safe tags/attributes survive.
     * - <img> src MUST point to the internal media endpoint (relative URL
     *   returned by the image upload). External or data: URIs are rejected —
     *   base64 must never reach persistence.
     * - Runs BEFORE saving: the HTML stored in the database is born clean.
     */
    public String sanitize(String rawHtml) {
        String cleaned = Jsoup.clean(rawHtml, "", Safelist.basicWithImages()
                .addAttributes("img", "src", "alt"));

        Document parsed = Jsoup.parseBodyFragment(cleaned);
        List<String> invalidSources = new ArrayList<>();

        for (Element img : parsed.select("img")) {
            String src = img.attr("src");
            if (src == null || !src.startsWith(MEDIA_PREFIX)) {
                invalidSources.add(src);
                img.remove();
            }
        }

        if (!invalidSources.isEmpty()) {
            throw new InvalidArticleContentException(
                    "Imagens devem ser enviadas pelo endpoint de mídia. Fontes inválidas: " + invalidSources);
        }

        return parsed.body().html();
    }

    /**
     * Extracts plain text for vector ingestion. <img> is discarded by
     * construction (never enters the text). Headings are prefixed to
     * preserve document structure inside chunks.
     */
    public String extractText(String sanitizedHtml) {
        Document doc = Jsoup.parseBodyFragment(sanitizedHtml);

        doc.select("img, script, style").remove();

        StringBuilder sb = new StringBuilder();
        doc.body().children().forEach(el -> {
            String tag = el.tagName();
            String text = el.text();
            if (text.isBlank()) {
                return;
            }
            if (tag.matches("h[1-6]")) {
                int level = tag.charAt(1) - '0';
                sb.append("#".repeat(level)).append(' ').append(text);
            } else {
                sb.append(text);
            }
            sb.append("\n\n");
        });

        return sb.toString().trim();
    }
}