package com.github.reedoverflow.stage1streader.service;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.utils.HTTPUtil;
import com.google.gson.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DiscuzService {

    /**
     * 板块列表
     */
    public List<Forum> getForumList() {
        String requestUrl = Config.URL + "api/mobile/index.php?version=4&module=forumindex&page=1";
        Gson gson = new Gson();
        List<Forum> forumList = new ArrayList<>();
        try {
            String result = HTTPUtil.doGet(requestUrl);
            JsonObject jsonResult = (JsonObject) JsonParser.parseString(result);
            JsonArray formArray = jsonResult.getAsJsonObject("Variables").getAsJsonArray("forumlist");

            Iterator it = formArray.iterator();
            while (it.hasNext()) {
                JsonElement element =(JsonElement) it.next();
                Forum forum = gson.fromJson(element, Forum.class);
                forumList.add(forum);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return forumList;
    }

    /**
     * 贴子列表
     */
    public List<Thread> getThreadList(int forumId, int page) {
        String requestUrl = Config.URL + "api/mobile/index.php?version=4&module=forumdisplay&fid="+forumId+"&page="+page;
        Gson gson = new Gson();
        List<Thread> threadList = new ArrayList<>();

        try {
            String result = HTTPUtil.doGet(requestUrl);
            JsonObject jsonResult = (JsonObject) JsonParser.parseString(result);
            JsonArray formArray = jsonResult.getAsJsonObject("Variables").getAsJsonArray("forum_threadlist");

            Iterator it = formArray.iterator();
            while (it.hasNext()) {
                JsonElement element =(JsonElement) it.next();
                Thread thread = gson.fromJson(element, Thread.class);
                threadList.add(thread);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return threadList;
    }

    /**
     * 回复列表
     */
    public List<Reply> getReplyList(int threadId, int page) {
        String requestUrl = Config.URL + "api/mobile/index.php?version=4&module=viewthread&tid="+threadId+"&page="+page;
        Gson gson = new Gson();
        List<Reply> replyList = new ArrayList<>();

        try {
            String result = HTTPUtil.doGet(requestUrl);
            JsonObject jsonResult = (JsonObject) JsonParser.parseString(result);
            JsonArray formArray = jsonResult.getAsJsonObject("Variables").getAsJsonArray("postlist");

            Iterator it = formArray.iterator();
            while (it.hasNext()) {
                JsonElement element =(JsonElement) it.next();
                Reply reply = gson.fromJson(element, Reply.class);
                replyList.add(reply);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return replyList;
    }
}
