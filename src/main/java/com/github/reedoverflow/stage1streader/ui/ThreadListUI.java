package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class ThreadListUI {

    private final ThreadPanel mainPanel;

    public ThreadListUI(Project project) {
        mainPanel = new ThreadPanel(project);
    }

    public JComponent createComponent() {
        return mainPanel;
    }

    public void getThreadByForumId(int forumId) {
        mainPanel.getThreadByForumId(forumId);
    }
}
