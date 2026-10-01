package com.github.reedoverflow.stage1streader.domain;

import java.util.List;
import java.util.Map;

/** Paging metadata comes from Discuz, including the user's personal page size. */
public final class ForumPage<T> {
    public final List<T> items;
    public final int page;
    public final int maxPage;
    public int forumId;
    public String subject = "";
    public Map<String, String> threadTypes;
    public boolean typeRequired;

    public ForumPage(List<T> items, int page, int maxPage) {
        this.items = items;
        this.page = page;
        this.maxPage = Math.max(page, maxPage);
    }
}
