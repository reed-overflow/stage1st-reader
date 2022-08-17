package com.github.reedoverflow.stage1streader.domain;

import java.util.List;

public class Forum {

    private String	fid;
    private String	name;
    private String	threads;
    private String	posts;
    private String	todayposts;
    private String	description;
    private List<Forum>	sublist;

    public String getFid() {
        return this.fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThreads() {
        return this.threads;
    }

    public void setThreads(String threads) {
        this.threads = threads;
    }

    public String getPosts() {
        return this.posts;
    }

    public void setPosts(String posts) {
        this.posts = posts;
    }

    public String getTodayposts() {
        return this.todayposts;
    }

    public void setTodayposts(String todayposts) {
        this.todayposts = todayposts;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Forum> getSublist() {
        return this.sublist;
    }

    public void setSublist(List<Forum> sublist) {
        this.sublist = sublist;
    }

}
