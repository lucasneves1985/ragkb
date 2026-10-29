package br.lcn.ragkb.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.exception.InvalidArticleContentException;

@Service
public class ArticleSanitizationService {

    private static final String MEDIA_PREFIX = "/api/media/articles/";
    private static final Pattern COLOR = Pattern.compile("(?i)(#[0-9a-f]{3,8}|[a-z]{1,20}|rgba?\\([0-9.,%\\s]+\\))");
    private static final Pattern FONT_SIZE = Pattern.compile("(?i)([8-9]|[1-6][0-9]|7[0-2])px");
    private static final Pattern FONT_FAMILY = Pattern.compile("[a-zA-Z0-9 ,\\\"'-]{1,100}");

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
        Document source = Jsoup.parseBodyFragment(rawHtml);
        sanitizeInlineStyles(source);
        Safelist safelist = Safelist.basic()
                .addTags("img", "mark", "hr")
                .addAttributes("img", "src", "alt", "height", "width")
                .addAttributes("a", "target", "rel")
                .addAttributes("p", "style")
                .addAttributes("h1", "style").addAttributes("h2", "style")
                .addAttributes("h3", "style").addAttributes("h4", "style")
                .addAttributes("h5", "style").addAttributes("h6", "style")
                .addAttributes("span", "style").addAttributes("mark", "style")
                .addAttributes("blockquote", "style").addAttributes("li", "style")
                .addAttributes("ul", "style").addAttributes("ol", "style")
                .addAttributes("pre", "style").addAttributes("code", "style");
        String cleaned = Jsoup.clean(source.body().html(), "", safelist);

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

    /** Keep only the formatting properties supported by the article editor. */
    private void sanitizeInlineStyles(Document document) {
        for (Element element : document.select("[style]")) {
            Map<String, String> safe = new LinkedHashMap<>();
            for (String declaration : element.attr("style").split(";")) {
                int colon = declaration.indexOf(':');
                if (colon < 1) continue;
                String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = declaration.substring(colon + 1).trim();
                if (isSafeStyle(property, value)) safe.put(property, value);
            }
            if (safe.isEmpty()) element.removeAttr("style");
            else element.attr("style", safe.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .reduce((left, right) -> left + "; " + right).orElse(""));
        }
    }

    private boolean isSafeStyle(String property, String value) {
        return switch (property) {
            case "color", "background-color" -> COLOR.matcher(value).matches();
            case "font-size" -> FONT_SIZE.matcher(value).matches();
            case "font-family" -> FONT_FAMILY.matcher(value).matches();
            case "text-align" -> List.of("left", "center", "right", "justify").contains(value.toLowerCase(Locale.ROOT));
            default -> false;
        };
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
