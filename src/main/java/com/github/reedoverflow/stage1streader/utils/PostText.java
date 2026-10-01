package com.github.reedoverflow.stage1streader.utils;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

/** Text-only rendering: no remote resource loading, scripts, or Swing HTML execution. */
public final class PostText {
    private PostText() { }
    public static String plain(String html, String root) {
        if (html == null || html.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                private int hidden;
                private final Deque<String> links = new ArrayDeque<>();
                @Override public void handleText(char[] data, int pos) {
                    if (hidden == 0) out.append(data);
                }
                @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    if (tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE) { hidden++; return; }
                    if (hidden > 0) return;
                    if (tag == HTML.Tag.A) links.push(url(attrs.getAttribute(HTML.Attribute.HREF), root));
                    if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.BLOCKQUOTE
                            || tag == HTML.Tag.PRE || tag == HTML.Tag.LI || tag == HTML.Tag.TR) out.append('\n');
                }
                @Override public void handleEndTag(HTML.Tag tag, int pos) {
                    if (tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE) { hidden = Math.max(0, hidden - 1); return; }
                    if (hidden > 0) return;
                    if (tag == HTML.Tag.A && !links.isEmpty()) {
                        String link = links.pop();
                        if (!link.isEmpty()) out.append(" <").append(link).append('>');
                    }
                    if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.BLOCKQUOTE
                            || tag == HTML.Tag.PRE || tag == HTML.Tag.LI || tag == HTML.Tag.TR) out.append('\n');
                }
                @Override public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    if (hidden > 0) return;
                    if (tag == HTML.Tag.BR || tag == HTML.Tag.HR) out.append('\n');
                    if (tag == HTML.Tag.IMG) {
                        Object src = attrs.getAttribute(HTML.Attribute.SRC);
                        if (attrs.getAttribute("file") != null) src = attrs.getAttribute("file");
                        out.append("[图片");
                        Object alt = attrs.getAttribute(HTML.Attribute.ALT);
                        if (alt != null) out.append(": ").append(alt);
                        String link = url(src, root);
                        if (!link.isEmpty()) out.append(' ').append(link);
                        out.append(']');
                    }
                }
            }, true);
        } catch (IOException ignored) { return html; }
        return out.toString().replace('\u00a0', ' ').replaceAll("\\n[\\t ]*\\n(?:[\\t ]*\\n)+", "\n\n").trim();
    }

    private static String url(Object value, String root) {
        if (value == null) return "";
        try {
            URI uri = URI.create(root).resolve(value.toString());
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())
                    ? uri.toASCIIString() : "";
        } catch (IllegalArgumentException ignored) { return ""; }
    }
}
