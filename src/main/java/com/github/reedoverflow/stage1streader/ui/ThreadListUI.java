package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;

import javax.swing.*;

public class ThreadListUI {

    private final ThreadPanel mainPanel;

    public ThreadListUI() {
        mainPanel = new ThreadPanel();
    }

    public JComponent createComponent() {
        return mainPanel;
    }

    public void getThreadByForumId(int forumId) {
        mainPanel.getThreadByForumId(forumId);
    }
}
