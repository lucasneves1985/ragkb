package br.lcn.ragkb.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.exception.InvalidArticleContentException;

@Service
public class ArticleSanitizationService {

    private static final String MEDIA_PREFIX = "/api/media/articles/";

    /**
     * Sanitizes rich-text HTML from the editor before persistence.
     *
     * IMPORTANT — do NOT use Safelist.basicWithImages() here: it restricts
     * img[src] to http/https protocols and silently STRIPS the attribute from
     * relative URLs (/api/media/articles/...), which are exactly the valid
     * ones returned by the media endpoint. Instead we build the whitelist
     * without protocol restriction on src — XSS protection stays with the
     * Safelist (tags/attributes/on* handlers), and src validation is fully
     * owned by the MEDIA_PREFIX check below, which rejects anything outside
     * the internal media endpoint (external URLs and data: URIs included).
     *
     * Runs BEFORE saving: the HTML stored in the database is born clean.
     */
    public String sanitize(String rawHtml) {
        String cleaned = Jsoup.clean(rawHtml, "", Safelist.basic()
                .addTags("img")
                .addAttributes("img", "src", "alt", "height", "width"));

        Document parsed = Jsoup.parseBodyFragment(cleaned);
        List<String> invalidSources = new ArrayList<>();

        for (Element img : parsed.select("img")) {
            String src = img.attr("src");
            if (src == null || src.isBlank() || !src.startsWith(MEDIA_PREFIX)) {
                invalidSources.add(describeSource(src));
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

    private String describeSource(String src) {
        if (src == null || src.isBlank()) {
            return "(src removido ou vazio — provável URL relativa descartada)";
        }
        return src;
    }
}