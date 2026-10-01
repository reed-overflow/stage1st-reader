package com.github.reedoverflow.stage1streader.utils;

import com.github.reedoverflow.stage1streader.domain.StoredCookie;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One isolated cookie jar per site/account. Never retries a write or follows a redirect. */
public final class HTTPUtil implements Closeable {
    private final String root;
    private final BasicCookieStore cookies = new BasicCookieStore();
    private final CloseableHttpClient client;

    public HTTPUtil(String root) {
        this.root = root;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(15000)
                .setConnectionRequestTimeout(15000).setSocketTimeout(30000)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        client = HttpClients.custom().setDefaultRequestConfig(config).setDefaultCookieStore(cookies)
                .disableAutomaticRetries().disableRedirectHandling()
                .setUserAgent("Stage1st-Reader/0.0.35 (IntelliJ; Discuz mobile API)").build();
    }

    public String get(String path) throws IOException { return execute(new HttpGet(root + path)); }

    public List<StoredCookie> snapshotCookies() {
        cookies.clearExpired(new Date());
        List<StoredCookie> result = new ArrayList<>();
        cookies.getCookies().forEach(cookie -> result.add(StoredCookie.from(cookie)));
        return result;
    }

    public void restoreCookies(List<StoredCookie> saved) {
        if (saved == null) return;
        String host = URI.create(root).getHost();
        Date now = new Date();
        for (StoredCookie stored : saved) {
            if (stored == null) continue;
            BasicClientCookie cookie = stored.restore(host, now);
            if (cookie != null) cookies.addCookie(cookie);
        }
    }

    public String post(String path, Map<String, String> fields, Charset charset) throws IOException {
        HttpPost post = new HttpPost(root + path);
        List<BasicNameValuePair> pairs = new ArrayList<>();
        fields.forEach((key, value) -> pairs.add(new BasicNameValuePair(key, value)));
        post.setEntity(new UrlEncodedFormEntity(pairs, charset));
        URI uri = URI.create(root);
        post.setHeader("Origin", uri.getScheme() + "://" + uri.getRawAuthority());
        return execute(post);
    }

    private String execute(HttpRequestBase request) throws IOException {
        request.setHeader("Accept", "application/json, text/plain, */*");
        request.setHeader("Referer", root);
        request.setHeader("Cache-Control", "no-cache");
        try (CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 300 && status < 400) {
                throw new IOException("论坛发生重定向，请将设置中的 URL 改为浏览器中的最终论坛根地址。");
            }
            if (status < 200 || status >= 300) throw new IOException("论坛返回 HTTP " + status);
            return response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally { request.releaseConnection(); }
    }

    /** Imports a Cookie request header, not Set-Cookie attributes. Values stay URL-encoded. */
    public void importCookies(String header) {
        String value = header.trim().replaceFirst("(?i)^Cookie:\\s*", "");
        if (value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException("请只粘贴单行 Cookie 请求头。");
        }
        URI uri = URI.create(root);
        int count = 0;
        for (String pair : value.split(";")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) continue;
            String name = pair.substring(0, equals).trim();
            if (!name.matches("[!#$%&'*+.^_`|~0-9a-zA-Z-]+")) {
                throw new IllegalArgumentException("Cookie 名称无效。");
            }
            if (Arrays.asList("path", "domain", "expires", "max-age", "samesite").contains(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("需要浏览器请求的 Cookie 头，不是响应的 Set-Cookie 头。");
            }
            BasicClientCookie cookie = new BasicClientCookie(name, pair.substring(equals + 1).trim());
            cookie.setDomain(uri.getHost());
            cookie.setPath(uri.getPath());
            cookie.setSecure("https".equalsIgnoreCase(uri.getScheme()));
            cookies.addCookie(cookie);
            count++;
        }
        if (count == 0) throw new IllegalArgumentException("未找到 Cookie。");
    }

    @Override public void close() throws IOException { cookies.clear(); client.close(); }
}
