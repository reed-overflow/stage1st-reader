package com.github.reedoverflow.stage1streader.constant;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Persistent application settings for the reader.
 *
 * <p>The old implementation exposed a mutable, hard-coded URL.  Keeping the
 * value in an IntelliJ application service means every project and every
 * request observes the value configured by the user.</p>
 */
@State(name = "Stage1stReaderConfig", storages = @Storage("stage1st-reader.xml"))
public class Config implements PersistentStateComponent<Config.SettingsState> {

    public static final String DEFAULT_URL = "https://stage1st.com/2b/";

    public static class SettingsState {
        public volatile String url = DEFAULT_URL;
    }

    private volatile SettingsState state = new SettingsState();

    public static Config getInstance() {
        return ServiceManager.getService(Config.class);
    }

    public String getUrl() {
        String normalized = normalizeUrl(state.url);
        return normalized.length() == 0 ? DEFAULT_URL : normalized;
    }

    public void setUrl(String url) {
        String normalized = normalizeUrl(url);
        state.url = normalized.length() == 0 ? DEFAULT_URL : normalized;
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
        state.url = loadedUrl == null || loadedUrl.trim().length() == 0
                ? DEFAULT_URL
                : normalizeUrl(loadedUrl);
        this.state = state;
    }
}
