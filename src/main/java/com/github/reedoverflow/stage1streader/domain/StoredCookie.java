package com.github.reedoverflow.stage1streader.domain;

import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import java.util.Date;

/** XML-serializable cookie, including the scope and lifetime of the original cookie. */
public final class StoredCookie {
    public String name = "";
    public String value = "";
    public String domain = "";
    public String path = "/";
    public long expires = -1;
    public boolean secure;
    public int version;
    public boolean hostOnly = true;
    public boolean pathAttribute;
    public boolean httpOnly;

    public static StoredCookie from(Cookie cookie) {
        StoredCookie stored = new StoredCookie();
        stored.name = cookie.getName();
        stored.value = cookie.getValue();
        stored.domain = cookie.getDomain();
        stored.path = cookie.getPath();
        stored.expires = cookie.getExpiryDate() == null ? -1 : cookie.getExpiryDate().getTime();
        stored.secure = cookie.isSecure();
        stored.version = cookie.getVersion();
        if (cookie instanceof ClientCookie) {
            ClientCookie client = (ClientCookie) cookie;
            stored.hostOnly = !client.containsAttribute(ClientCookie.DOMAIN_ATTR);
            stored.pathAttribute = client.containsAttribute(ClientCookie.PATH_ATTR);
            stored.httpOnly = client.containsAttribute("httponly");
        }
        return stored;
    }

    public BasicClientCookie restore(String host, Date now) {
        if (name == null || !name.matches("[!#$%&'*+.^_`|~0-9a-zA-Z-]+")
                || value == null || value.contains("\r") || value.contains("\n")
                || domain == null || domain.isEmpty() || path == null || !path.startsWith("/")
                || expires != -1 && expires <= now.getTime()) return null;
        String scopedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
        host = host.toLowerCase(java.util.Locale.ROOT);
        scopedDomain = scopedDomain.toLowerCase(java.util.Locale.ROOT);
        if (!host.equals(scopedDomain) && (hostOnly || !host.endsWith("." + scopedDomain))) return null;
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setSecure(secure);
        cookie.setVersion(version);
        if (expires != -1) cookie.setExpiryDate(new Date(expires));
        if (!hostOnly) cookie.setAttribute(ClientCookie.DOMAIN_ATTR, domain);
        if (pathAttribute) cookie.setAttribute(ClientCookie.PATH_ATTR, path);
        if (httpOnly) cookie.setAttribute("httponly", "");
        return cookie;
    }
}
