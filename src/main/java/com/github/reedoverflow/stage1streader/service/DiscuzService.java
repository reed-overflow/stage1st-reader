package com.github.reedoverflow.stage1streader.service;

import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.domain.ForumPage;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.utils.PostText;
import com.google.gson.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Discuz mobile v4 protocol. Each operation is bound to a particular site/account. */
public class DiscuzService {
    private final SessionManager.Session session;
    private Charset charset = StandardCharsets.UTF_8;

    public DiscuzService() { this(SessionManager.getInstance().current()); }
    private DiscuzService(SessionManager.Session session) { this.session = session; }
    public boolean isCurrent() { return SessionManager.getInstance().isCurrent(session); }
    public String root() { return session.root; }

    public List<Forum> getForumList() {
        synchronized (session) {
            checkReadRequest();
            return list(get("forumindex").variables, "forumlist", Forum.class);
        }
    }

    public ForumPage<Thread> getThreadPage(int forumId, int page) {
        requireId(forumId);
        synchronized (session) {
            checkReadRequest();
            Response r = get("forumdisplay", "fid", "" + forumId, "page", "" + Math.max(1, page));
            JsonObject v = r.variables;
            List<Thread> threads = list(v, "forum_threadlist", Thread.class);
            int actualPage = positive(v, "page", Math.max(1, page));
            int tpp = positive(v, "tpp", 0);
            long total = number(object(v, "forum"), "threadcount", -1);
            int max = tpp > 0 && total >= 0 ? pages(total, tpp)
                    : (threads.isEmpty() ? actualPage : safeNext(actualPage));
            ForumPage<Thread> result = new ForumPage<>(threads, actualPage, max);
            result.forumId = forumId;
            JsonObject types = object(v, "threadtypes");
            result.typeRequired = number(types, "required", 0) == 1;
            result.threadTypes = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : object(types, "types").entrySet()) {
                result.threadTypes.put(e.getKey(), text(e.getValue()));
            }
            return result;
        }
    }

    public ForumPage<Reply> getReplyPage(int threadId, int page) {
        requireId(threadId);
        synchronized (session) {
            checkReadRequest();
            Response r = get("viewthread", "tid", "" + threadId, "page", "" + Math.max(1, page));
            JsonObject v = r.variables;
            List<Reply> posts = list(v, "postlist", Reply.class);
            JsonObject thread = object(v, "thread");
            int ppp = positive(v, "ppp", 0);
            long replies = number(thread, "replies", -1);
            int actual = positive(v, "page", Math.max(1, page));
            int max = ppp > 0 && replies >= 0 ? pages(replies == Long.MAX_VALUE ? replies : replies + 1, ppp)
                    : (posts.isEmpty() ? actual : safeNext(actual));
            // Discuz can clamp an old bookmark to the new last page after posts are deleted.
            actual = Math.min(actual, max);
            ForumPage<Reply> result = new ForumPage<>(posts, actual, max);
            result.forumId = positive(thread, "fid", positive(v, "fid", 0));
            result.subject = text(thread.get("subject"));
            return result;
        }
    }

    public void login(String username, String password, String loginField, int question, String answer) {
        SessionManager.Session candidate = new SessionManager.Session(session.root);
        boolean installed = false;
        try {
            DiscuzService service = new DiscuzService(candidate);
            Response initial = service.get("login");
            Map<String, String> fields = fields("username", username, "password", password,
                    "loginfield", loginField, "questionid", "" + question, "answer", answer,
                    "cookietime", "2592000", "formhash", service.formhash(initial), "fastloginfield", loginField);
            Response result = service.post("login", fields, "loginsubmit", "yes");
            // UID from a subsequent request proves the cookie jar actually works.
            if (!result.code.isEmpty() && !loginSucceeded(result.code)) {
                throw new IllegalStateException(result.message);
            }
            service.get("login");
            if (!candidate.loggedIn()) throw new IllegalStateException("未获得登录会话。验证码、二次验证或风控请使用浏览器登录后导入 Cookie。");
            SessionManager.getInstance().install(candidate, session);
            installed = true;
        } finally {
            if (!installed) candidate.close();
        }
    }

    public void importCookies(String header) {
        SessionManager.Session candidate = new SessionManager.Session(session.root);
        boolean installed = false;
        try {
            candidate.http.importCookies(header);
            new DiscuzService(candidate).get("login");
            if (!candidate.loggedIn()) throw new IllegalStateException("Cookie 已失效或不属于当前站点，请重新复制完整 Cookie 请求头。");
            SessionManager.getInstance().install(candidate, session);
            installed = true;
        } finally {
            if (!installed) candidate.close();
        }
    }

    public static final class Submission {
        public final String message;
        public final int threadId;
        public final boolean moderated;
        Submission(String message, int threadId, boolean moderated) {
            this.message = message; this.threadId = threadId; this.moderated = moderated;
        }
    }

    public Submission submit(int forumId, int threadId, String subject, String message, String typeId) {
        if (message.trim().isEmpty()) throw new IllegalArgumentException("正文不能为空。");
        boolean reply = threadId > 0;
        requireId(reply ? threadId : forumId);
        synchronized (session) {
            if (!isCurrent()) throw new IllegalStateException("站点或账号已改变，请重新打开编辑器。");
            // Fetch a fresh hash using the same jar immediately before submitting.
            Response state = get("login");
            if (!session.loggedIn()) throw new IllegalStateException("请先登录；当前会话可能已过期。");
            Map<String, String> data = fields("formhash", formhash(state), "message", message,
                    "usesig", "0", "wysiwyg", "0", "subject", subject, "typeid", typeId);
            Response result;
            try {
                result = reply
                        ? post("sendreply", data, "tid", "" + threadId, "replysubmit", "yes")
                        : post("newthread", data, "fid", "" + forumId, "topicsubmit", "yes");
            } catch (RuntimeException e) {
                // Do not auto-resend: the server may have accepted the write before a timeout.
                throw new IllegalStateException("提交结果无法确认，请先刷新主题核对，避免重复发布。草稿已保留。", e);
            }
            String code = result.code.split("/")[0];
            boolean success = Arrays.asList("post_reply_succeed", "post_reply_mod_succeed",
                    "post_newthread_succeed", "post_newthread_mod_succeed").contains(code);
            if (!success) throw new IllegalStateException(result.message.isEmpty()
                    ? "接口未确认提交成功，请刷新核对。草稿已保留。" : result.message);
            return new Submission(result.message, positive(result.variables, "tid", threadId), code.contains("_mod_"));
        }
    }

    private String formhash(Response r) {
        String hash = text(r.variables.get("formhash"));
        if (hash.isEmpty()) throw new IllegalStateException("接口未返回 formhash，请刷新登录状态。");
        return hash;
    }

    private void checkReadRequest() {
        if (java.lang.Thread.currentThread().isInterrupted() || !isCurrent()) {
            throw new CancellationException("阅读请求已被替换");
        }
    }

    private Response get(String module, String... parameters) {
        try {
            Response r = parse(session.http.get(path(module, parameters)));
            boolean loggedInResponse = "login".equals(module) && loginSucceeded(r.code) && session.loggedIn();
            if ((!r.code.isEmpty() || !r.message.isEmpty()) && !loggedInResponse) {
                throw new IllegalStateException(r.message);
            }
            return r;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            SessionManager.getInstance().save(session);
        }
    }

    private Response post(String module, Map<String, String> fields, String... parameters) {
        try { return parse(session.http.post(path(module, parameters), fields, charset)); }
        catch (IOException e) { throw new IllegalStateException(e.getMessage(), e); }
        finally { SessionManager.getInstance().save(session); }
    }

    private Response parse(String body) {
        try {
            JsonElement parsed = new JsonParser().parse(body);
            if (!parsed.isJsonObject()) throw new JsonParseException("not an object");
            JsonObject root = parsed.getAsJsonObject();
            JsonObject variables = object(root, "Variables");
            JsonObject msg = object(root, "Message");
            String code = text(msg.get("messageval"));
            String message = text(msg.get("messagestr"));
            if (message.isEmpty()) message = text(root.get("Message"));
            if (message.isEmpty()) message = code;
            if (!root.has("Variables") && message.isEmpty()) {
                throw new JsonParseException("missing Variables");
            }
            String encoding = text(root.get("Charset"));
            if (!encoding.isEmpty()) charset = Charset.forName(encoding);
            if (variables.has("member_uid")) {
                boolean wasLoggedIn = session.loggedIn();
                session.uid = text(variables.get("member_uid"));
                session.username = text(variables.get("member_username"));
                if (wasLoggedIn && !session.loggedIn() && isCurrent()) ReaderEvents.fire(true);
            }
            return new Response(variables, code, PostText.plain(message, session.root));
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException e) {
            // Never include the raw response, credentials, cookies or HTML in diagnostic output.
            throw new IllegalStateException("论坛未返回有效的移动 API JSON，可能是验证码、风控或接口关闭。请在浏览器确认站点。");
        }
    }

    private static final class Response {
        final JsonObject variables;
        final String code;
        final String message;
        Response(JsonObject variables, String code, String message) {
            this.variables = variables; this.code = code; this.message = message;
        }
    }

    private static String path(String module, String... parameters) {
        StringBuilder url = new StringBuilder("api/mobile/index.php?version=4&module=").append(encode(module));
        for (int i = 0; i + 1 < parameters.length; i += 2) {
            url.append('&').append(encode(parameters[i])).append('=').append(encode(parameters[i + 1]));
        }
        return url.toString();
    }

    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException e) { throw new AssertionError(e); }
    }

    private static Map<String, String> fields(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }

    private static <T> List<T> list(JsonObject variables, String name, Class<T> type) {
        JsonElement raw = variables.get(name);
        if (raw == null || raw.isJsonNull()) throw new IllegalStateException("接口缺少 " + name + "，请检查登录权限或站点维护提示。");
        List<T> result = new ArrayList<>();
        Iterable<JsonElement> entries;
        if (raw.isJsonArray()) entries = raw.getAsJsonArray();
        else if (raw.isJsonObject()) {
            List<JsonElement> values = new ArrayList<>();
            raw.getAsJsonObject().entrySet().forEach(e -> values.add(e.getValue()));
            entries = values; // PHP sparse arrays may be encoded as objects.
        } else throw new IllegalStateException("列表格式不受支持：" + name);
        Gson gson = new Gson();
        try {
            for (JsonElement item : entries) if (item != null && !item.isJsonNull()) result.add(gson.fromJson(item, type));
        } catch (JsonParseException e) { throw new IllegalStateException("列表数据格式不受支持：" + name); }
        return result;
    }

    private static JsonObject object(JsonObject value, String name) {
        JsonElement child = value.get(name);
        return child != null && child.isJsonObject() ? child.getAsJsonObject() : new JsonObject();
    }
    private static String text(JsonElement value) {
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
    private static long number(JsonObject value, String name, long fallback) {
        try { return Long.parseLong(text(value.get(name))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private static int positive(JsonObject value, String name, int fallback) {
        long n = number(value, name, fallback);
        return n > 0 && n <= Integer.MAX_VALUE ? (int) n : fallback;
    }
    private static int pages(long total, int size) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, total / size + (total % size == 0 ? 0 : 1)));
    }
    private static int safeNext(int page) { return page == Integer.MAX_VALUE ? page : page + 1; }
    private static boolean loginSucceeded(String code) {
        return code.startsWith("login_succeed") || code.startsWith("location_login_succeed");
    }
    private static void requireId(int id) { if (id <= 0) throw new IllegalArgumentException("无效的版块／主题 ID。"); }
}
