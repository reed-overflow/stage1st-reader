package com.github.reedoverflow.stage1streader.service;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.utils.HTTPUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;

/** Application-wide sessions backed by local settings; passwords are never retained. */
public final class SessionManager implements Disposable {
    public static final class Session {
        public final String root;
        public final HTTPUtil http;
        public volatile String username = "";
        public volatile String uid = "0";
        public Session(String root) { this.root = root; this.http = new HTTPUtil(root); }
        public boolean loggedIn() { return !"0".equals(uid) && !uid.isEmpty(); }
        public void close() { try { http.close(); } catch (java.io.IOException ignored) { } }
    }

    private Session current;
    public static SessionManager getInstance() { return ServiceManager.getService(SessionManager.class); }

    public synchronized Session current() {
        String root = Config.getInstance().getUrl();
        if (current == null || !current.root.equals(root)) {
            if (current != null) current.close();
            current = new Session(root);
            Config.SavedSession saved = Config.getInstance().getSavedSession(root);
            current.http.restoreCookies(saved.cookies);
            if (!current.http.snapshotCookies().isEmpty()) {
                current.username = saved.username == null ? "" : saved.username;
                current.uid = saved.uid == null ? "0" : saved.uid;
            }
            save(current);
        }
        return current;
    }

    public synchronized boolean isCurrent(Session session) {
        return current == session && Config.getInstance().getUrl().equals(session.root);
    }

    public synchronized void install(Session candidate, Session expected) {
        if (!isCurrent(expected)) {
            candidate.close();
            throw new IllegalStateException("站点或账号已切换，请重新登录。");
        }
        current = candidate;
        save(current);
        expected.close();
        ReaderEvents.fire(true);
    }

    public synchronized void logout() {
        Config.getInstance().clearSession();
        if (current != null) current.close();
        current = new Session(Config.getInstance().getUrl());
        ReaderEvents.fire(true);
    }

    /** Stale requests and unverified login candidates must not overwrite saved credentials. */
    public synchronized void save(Session session) {
        if (!isCurrent(session)) return;
        Config.SavedSession saved = new Config.SavedSession();
        saved.root = session.root;
        if (session.loggedIn()) {
            saved.username = session.username;
            saved.uid = session.uid;
            saved.cookies = session.http.snapshotCookies();
        }
        Config.getInstance().saveSession(saved);
    }

    @Override public synchronized void dispose() {
        if (current != null) current.close();
        current = null;
    }
}
