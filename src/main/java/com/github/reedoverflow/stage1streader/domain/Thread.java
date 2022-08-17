package com.github.reedoverflow.stage1streader.domain;

import java.util.List;

public class Thread {
    private String	tid;
    private String	typeid;
    private String	readperm;
    private String	price;
    private String	author;
    private String	authorid;
    private String	subject;
    private String	dateline;
    private String	lastpost;
    private String	lastposter;
    private String	views;
    private String	replies;
    private String	displayorder;
    private String	digest;
    private String	special;
    private String	attachment;
    private String	recommend_add;
    private String	replycredit;
    private String	dbdateline;
    private String	dblastpost;
    private String	rushreply;
    private List<Reply> reply;

    public String getTid() {
        return this.tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getTypeid() {
        return this.typeid;
    }

    public void setTypeid(String typeid) {
        this.typeid = typeid;
    }

    public String getReadperm() {
        return this.readperm;
    }

    public void setReadperm(String readperm) {
        this.readperm = readperm;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorid() {
        return this.authorid;
    }

    public void setAuthorid(String authorid) {
        this.authorid = authorid;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDateline() {
        return this.dateline;
    }

    public void setDateline(String dateline) {
        this.dateline = dateline;
    }

    public String getLastpost() {
        return this.lastpost;
    }

    public void setLastpost(String lastpost) {
        this.lastpost = lastpost;
    }

    public String getLastposter() {
        return this.lastposter;
    }

    public void setLastposter(String lastposter) {
        this.lastposter = lastposter;
    }

    public String getViews() {
        return this.views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public String getReplies() {
        return this.replies;
    }

    public void setReplies(String replies) {
        this.replies = replies;
    }

    public String getDisplayorder() {
        return this.displayorder;
    }

    public void setDisplayorder(String displayorder) {
        this.displayorder = displayorder;
    }

    public String getDigest() {
        return this.digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getSpecial() {
        return this.special;
    }

    public void setSpecial(String special) {
        this.special = special;
    }

    public String getAttachment() {
        return this.attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getRecommend_add() {
        return this.recommend_add;
    }

    public void setRecommend_add(String recommend_add) {
        this.recommend_add = recommend_add;
    }

    public String getReplycredit() {
        return this.replycredit;
    }

    public void setReplycredit(String replycredit) {
        this.replycredit = replycredit;
    }

    public String getDbdateline() {
        return this.dbdateline;
    }

    public void setDbdateline(String dbdateline) {
        this.dbdateline = dbdateline;
    }

    public String getDblastpost() {
        return this.dblastpost;
    }

    public void setDblastpost(String dblastpost) {
        this.dblastpost = dblastpost;
    }

    public String getRushreply() {
        return this.rushreply;
    }

    public void setRushreply(String rushreply) {
        this.rushreply = rushreply;
    }

    public List<Reply> getReply() {
        return this.reply;
    }

    public void setReply(List<Reply> reply) {
        this.reply = reply;
    }

}
