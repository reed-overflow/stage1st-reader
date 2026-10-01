package com.github.reedoverflow.stage1streader.service;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.domain.StoredCookie;
import com.github.reedoverflow.stage1streader.utils.HTTPUtil;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.RFC6265LaxSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SessionPersistenceTest {
    private Disposable disposable;
    private Config config;
    private SessionManager manager;
    private HttpServer server;
    private final AtomicBoolean expired = new AtomicBoolean();
    private final AtomicReference<String> nextCookie = new AtomicReference<>();
    private final AtomicReference<String> receivedCookie = new AtomicReference<>();

    @Before public void setUp() throws Exception {
        disposable = Disposer.newDisposable();
        MockApplication application = new MockApplication(disposable);
        ApplicationManager.setApplication(application, disposable);
        config = new Config();
        manager = new SessionManager();
        application.registerService(Config.class, config);
        application.registerService(SessionManager.class, manager);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/2b/api/mobile/index.php", exchange -> {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            receivedCookie.set(cookie);
            boolean login = "POST".equals(exchange.getRequestMethod());
            boolean authenticated = !expired.get() && (login || cookie != null && cookie.contains("auth=valid"));
            String updated = login ? "auth=valid; Path=/2b/; Max-Age=2592000; HttpOnly" : nextCookie.getAndSet(null);
            if (updated != null) exchange.getResponseHeaders().add("Set-Cookie", updated);
            String json = "{\"Variables\":{\"member_uid\":\"" + (authenticated ? "42" : "0")
                    + "\",\"member_username\":\"" + (authenticated ? "reader" : "")
                    + "\",\"formhash\":\"fixture\",\"forumlist\":[]}}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        config.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/2b/");
    }

    @After public void tearDown() throws Exception {
        if (server != null) server.stop(0);
        // Finish queued reader notifications before removing the mock application.
        SwingUtilities.invokeAndWait(() -> { });
        if (manager != null) manager.dispose();
        if (disposable != null) Disposer.dispose(disposable);
    }

    @Test public void passwordLoginSurvivesSettingsRoundTrip() throws Exception {
        new DiscuzService().login("reader", "test-password", "username", 0, "test-answer");
        String xml = restart();
        assertFalse(xml.contains("test-password"));
        assertFalse(xml.contains("test-answer"));
        assertEquals("42", manager.current().uid);
        assertEquals("reader", manager.current().username);
        new DiscuzService().getForumList();
        assertTrue(receivedCookie.get().contains("auth=valid"));
        assertTrue(manager.current().loggedIn());
    }

    @Test public void importedSessionCookiesSurviveSettingsRoundTrip() throws Exception {
        new DiscuzService().importCookies("auth=valid%2Fencoded==; saltkey=fixture");
        restart();
        new DiscuzService().getForumList();
        assertTrue(receivedCookie.get().contains("auth=valid%2Fencoded=="));
        assertTrue(receivedCookie.get().contains("saltkey=fixture"));
    }

    @Test public void refreshedCookiesAreSavedAfterRequests() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        nextCookie.set("auth=valid-refreshed; Path=/2b/; HttpOnly");
        new DiscuzService().getForumList();
        restart();
        new DiscuzService().getForumList();
        assertTrue(receivedCookie.get().contains("auth=valid-refreshed"));
    }

    @Test public void failedImportDoesNotReplaceSavedLogin() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        try {
            new DiscuzService().importCookies("auth=invalid");
            fail("Invalid cookies must not install a session");
        } catch (IllegalStateException expected) {
            assertTrue(manager.current().loggedIn());
        }
        restart();
        new DiscuzService().getForumList();
        assertTrue(receivedCookie.get().contains("auth=valid"));
    }

    @Test public void logoutCannotBeUndoneByAnOldRequestOrRestart() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        SessionManager.Session old = manager.current();
        manager.logout();
        old.http.importCookies("auth=valid-late-response");
        manager.save(old);
        assertTrue(config.getState().session.cookies.isEmpty());
        restart();
        assertFalse(manager.current().loggedIn());
        assertTrue(manager.current().http.snapshotCookies().isEmpty());
    }

    @Test public void changingSitesClearsSavedLoginIncludingWhenSwitchingBack() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        String original = config.getUrl();
        SessionManager.Session old = manager.current();
        config.setUrl("https://other.example/2b/");
        manager.save(old);
        assertFalse(manager.current().loggedIn());
        assertTrue(manager.current().http.snapshotCookies().isEmpty());
        config.setUrl(original);
        restart();
        assertFalse(manager.current().loggedIn());
    }

    @Test public void serverGuestResponseClearsSavedLogin() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        expired.set(true);
        new DiscuzService().getForumList();
        assertTrue(config.getState().session.cookies.isEmpty());
        restart();
        assertFalse(manager.current().loggedIn());
    }

    @Test public void restoreDropsExpiredAndForeignCookies() throws Exception {
        new DiscuzService().importCookies("auth=valid");
        config.getState().session.cookies.get(0).expires = System.currentTimeMillis() - 1000;
        restart();
        assertFalse(manager.current().loggedIn());
        try (HTTPUtil http = new HTTPUtil("https://stage1st.com/2b/")) {
            StoredCookie foreign = cookie("other.example", "/2b/", false);
            StoredCookie expiredCookie = cookie("stage1st.com", "/2b/", false);
            expiredCookie.expires = 1;
            http.restoreCookies(Arrays.asList(null, foreign, expiredCookie));
            assertTrue(http.snapshotCookies().isEmpty());
        }
    }

    @Test public void roundTripPreservesCookieScopeFlagsAndExpiry() {
        StoredCookie stored = cookie("stage1st.com", "/2b/", true);
        stored.expires = System.currentTimeMillis() + 3600000;
        stored.httpOnly = true;
        stored.pathAttribute = true;
        StoredCookie loaded = XmlSerializer.deserialize(XmlSerializer.serialize(stored), StoredCookie.class);
        BasicClientCookie restored = loaded.restore("stage1st.com", new Date());
        assertNotNull(restored);
        assertEquals(stored.expires, restored.getExpiryDate().getTime());
        assertTrue(restored.containsAttribute("httponly"));
        assertTrue(restored.containsAttribute(ClientCookie.PATH_ATTR));
        assertFalse(restored.containsAttribute(ClientCookie.DOMAIN_ATTR));
        RFC6265LaxSpec spec = new RFC6265LaxSpec();
        assertTrue(spec.match(restored, new CookieOrigin("stage1st.com", 443, "/2b/api/mobile/index.php", true)));
        assertFalse(spec.match(restored, new CookieOrigin("stage1st.com", 80, "/2b/", false)));
        assertFalse(spec.match(restored, new CookieOrigin("stage1st.com", 443, "/another/", true)));
        assertFalse(spec.match(restored, new CookieOrigin("sub.stage1st.com", 443, "/2b/", true)));
        loaded.hostOnly = false;
        restored = loaded.restore("sub.stage1st.com", new Date());
        assertNotNull(restored);
        assertTrue(spec.match(restored, new CookieOrigin("sub.stage1st.com", 443, "/2b/", true)));
    }

    @Test public void oldSettingsWithoutSessionRemainCompatible() throws Exception {
        config.loadState(XmlSerializer.deserialize(JDOMUtil.load("<SettingsState/>"), Config.SettingsState.class));
        assertFalse(manager.current().loggedIn());
        assertTrue(manager.current().http.snapshotCookies().isEmpty());
        assertEquals(Config.DEFAULT_URL, config.getUrl());
    }

    private String restart() throws Exception {
        String xml = JDOMUtil.writeElement(XmlSerializer.serialize(config.getState()));
        manager.dispose();
        config.loadState(XmlSerializer.deserialize(JDOMUtil.load(xml), Config.SettingsState.class));
        manager.current();
        return xml;
    }

    private static StoredCookie cookie(String domain, String path, boolean secure) {
        StoredCookie stored = new StoredCookie();
        stored.name = "auth";
        stored.value = "fixture";
        stored.domain = domain;
        stored.path = path;
        stored.secure = secure;
        return stored;
    }
}
