package com.github.reedoverflow.stage1streader.constant;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import com.github.reedoverflow.stage1streader.service.ReaderEvents;
import com.github.reedoverflow.stage1streader.domain.StoredCookie;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent application settings for the reader.
 *
 * <p>The old implementation exposed a mutable, hard-coded URL.  Keeping the
 * value in an IntelliJ application service means every project and every
 * request observes the value configured by the user.</p>
 */
@State(name = "Stage1stReaderConfig", storages = @Storage(value = "stage1st-reader.xml", roamingType = RoamingType.DISABLED))
public class Config implements PersistentStateComponent<Config.SettingsState> {

    public static final String DEFAULT_URL = "https://stage1st.com/2b/";

    public static class SettingsState {
        public volatile String url = DEFAULT_URL;
        public boolean camouflage = true;
        public List<Bookmark> bookmarks = new ArrayList<>();
        public volatile SavedSession session = new SavedSession();
    }

    public static class SavedSession {
        public String root = "";
        public String username = "";
        public String uid = "0";
        public List<StoredCookie> cookies = new ArrayList<>();
    }

    public synchronized SavedSession getSavedSession(String root) {
        return state.session != null && root.equals(state.session.root) && root.equals(getUrl())
                ? state.session : new SavedSession();
    }

    public synchronized void saveSession(SavedSession session) {
        if (getUrl().equals(session.root)) state.session = session;
    }

    public synchronized void clearSession() {
        state.session = new SavedSession();
    }

    public static class Bookmark {
        public String root = "";
        public int threadId;
        public int page = 1;
        public String title = "";
    }

    public synchronized List<Bookmark> getBookmarks() {
        List<Bookmark> result = new ArrayList<>();
        if (state.bookmarks != null) for (Bookmark mark : state.bookmarks) {
            if (mark != null && getUrl().equals(mark.root) && mark.threadId > 0 && mark.page > 0) result.add(mark);
        }
        return result;
    }

    public synchronized void saveBookmark(int tid, int page, String title) {
        if (state.bookmarks == null) state.bookmarks = new ArrayList<>();
        removeBookmark(tid);
        Bookmark mark = new Bookmark();
        mark.root = getUrl(); mark.threadId = tid; mark.page = page; mark.title = title;
        state.bookmarks.add(mark);
    }

    public synchronized void removeBookmark(int tid) {
        if (state.bookmarks != null) state.bookmarks.removeIf(b ->
                b != null && getUrl().equals(b.root) && b.threadId == tid);
    }

    private volatile SettingsState state = new SettingsState();

    public static Config getInstance() {
        return ApplicationManager.getApplication().getService(Config.class);
    }

    public String getUrl() {
        String normalized = normalizeUrl(state.url);
        return normalized.length() == 0 ? DEFAULT_URL : normalized;
    }

    public void setUrl(String url) {
        String normalized = normalizeUrl(url);
        if (!isValidUrl(normalized)) throw new IllegalArgumentException("Invalid forum root URL");
        boolean changed;
        synchronized (this) {
            changed = !getUrl().equals(normalized);
            state.url = normalized;
            if (changed) clearSession();
        }
        if (changed) ReaderEvents.fire(true);
    }

    public boolean isCamouflage() { return state.camouflage; }

    public void setCamouflage(boolean value) {
        state.camouflage = value;
        ReaderEvents.fire(false);
    }

    /**
     * Normalizes the forum root so callers can safely append the API path.
     */
    public static String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() == 0) {
            return "";
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/";
    }

    /**
     * Only HTTP(S) forum roots are supported by the current HTTP client.
     */
    public static boolean isValidUrl(String value) {
        String normalized = normalizeUrl(value);
        if (normalized.length() == 0) {
            return false;
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535)
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public SettingsState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        String loadedUrl = state.url;
        state.url = !isValidUrl(loadedUrl)
                ? DEFAULT_URL
                : normalizeUrl(loadedUrl);
        this.state = state;
    }
}
