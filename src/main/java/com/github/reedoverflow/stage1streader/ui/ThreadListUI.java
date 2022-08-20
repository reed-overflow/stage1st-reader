package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;

import javax.swing.*;

public class ThreadListUI {

    private static ThreadListUI instance;
    private ThreadPanel mainPanel;

    static {
        instance = new ThreadListUI();
    }

    private ThreadListUI() {
        mainPanel = new ThreadPanel();
    }

    public static ThreadListUI getInstance() {
        return instance;
    }

    public JComponent createComponent() {
        return mainPanel;
    }

    public void getThreadByForumId(int forumId) {
        mainPanel.getThreadByForumId(forumId);
    }
}
