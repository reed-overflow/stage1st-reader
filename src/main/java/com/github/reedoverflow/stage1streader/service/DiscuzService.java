package com.github.reedoverflow.stage1streader.service;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.utils.HTTPUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscuzService {

    private static final String API_PATH = "api/mobile/index.php";

    /**
     * Forum list.
     */
    public List<Forum> getForumList() {
        String requestUrl = buildApiUrl("forumindex", "page", "1");
        return requestList(requestUrl, "forumlist", Forum.class, "forum list");
    }

    /**
     * Thread list for a forum.
     */
    public List<Thread> getThreadList(int forumId, int page) {
        if (forumId <= 0) {
            throw new IllegalArgumentException("forumId must be greater than zero");
        }

        String requestUrl = buildApiUrl(
                "forumdisplay",
                "fid", String.valueOf(forumId),
                "page", String.valueOf(normalizePage(page))
        );
        return requestList(requestUrl, "forum_threadlist", Thread.class, "thread list");
    }

    /**
     * Reply/post list for a thread.
     */
    public List<Reply> getReplyList(int threadId, int page) {
        if (threadId <= 0) {
            throw new IllegalArgumentException("threadId must be greater than zero");
        }

        String requestUrl = buildApiUrl(
                "viewthread",
                "tid", String.valueOf(threadId),
                "page", String.valueOf(normalizePage(page))
        );
        return requestList(requestUrl, "postlist", Reply.class, "reply list");
    }

    private int normalizePage(int page) {
        return Math.max(1, page);
    }

    private String buildApiUrl(String module, String... parameters) {
        String baseUrl = Config.getInstance().getUrl();
        if (!Config.isValidUrl(baseUrl)) {
            throw new IllegalStateException("The configured forum URL is invalid: " + baseUrl);
        }

        StringBuilder url = new StringBuilder(baseUrl)
                .append(API_PATH)
                .append("?version=4&module=")
                .append(module);

        for (int i = 0; i + 1 < parameters.length; i += 2) {
            url.append('&')
                    .append(parameters[i])
                    .append('=')
                    .append(parameters[i + 1]);
        }
        return url.toString();
    }

    private <T> List<T> requestList(String requestUrl, String arrayName, Class<T> itemType, String description) {
        try {
            String result = HTTPUtil.doGet(requestUrl);
            JsonElement rootElement = new JsonParser().parse(result);
            if (rootElement == null || !rootElement.isJsonObject()) {
                throw new IOException("The forum returned an invalid JSON response.");
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonObject variables = getObject(root, "Variables");
            if (variables == null) {
                throw new IOException(readApiError(root));
            }

            JsonElement listElement = variables.get(arrayName);
            if (listElement == null || listElement.isJsonNull()) {
                String apiMessage = readApiMessage(root);
                if (apiMessage != null) {
                    throw new IOException(apiMessage);
                }
                return Collections.emptyList();
            }
            if (!listElement.isJsonArray()) {
                throw new IOException("Unexpected response format for " + description + ".");
            }

            Gson gson = new Gson();
            JsonArray items = listElement.getAsJsonArray();
            List<T> resultList = new ArrayList<>(items.size());
            for (JsonElement item : items) {
                if (item == null || item.isJsonNull()) {
                    continue;
                }
                resultList.add(gson.fromJson(item, itemType));
            }
            return resultList;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new RuntimeException("Unable to load " + description + ": " + e.getMessage(), e);
        }
    }

    private JsonObject getObject(JsonObject source, String name) {
        JsonElement element = source.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String readApiError(JsonObject root) {
        String message = readApiMessage(root);
        return message == null ? "The forum response did not contain Variables." : message;
    }

    private String readApiMessage(JsonObject root) {
        JsonElement rawMessage = root.get("Message");
        if (rawMessage != null && rawMessage.isJsonPrimitive()) {
            return rawMessage.getAsString();
        }

        JsonObject message = getObject(root, "Message");
        if (message != null) {
            String messageText = primitiveText(message.get("messagestr"));
            if (messageText != null) {
                return messageText;
            }
            String messageCode = primitiveText(message.get("messageval"));
            if (messageCode != null) {
                return messageCode;
            }
        }
        return null;
    }

    private String primitiveText(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
