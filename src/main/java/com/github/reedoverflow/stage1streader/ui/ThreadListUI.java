package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;
import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;

import javax.swing.*;
import java.awt.*;

public class ThreadListUI {

    private static ThreadListUI instance;
    private ThreadPanel mainPanel;

    static {
        instance = new ThreadListUI();
    }

    private ThreadListUI() {
        mainPanel = new ThreadPanel();
//        mainPanel.setLayout(new BorderLayout());
//        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    public static ThreadListUI getInstance() {
        return instance;
    }

    public JComponent createComponent() {
        return mainPanel;
    }

    public void setPanelText(String text) {
        mainPanel.setText(text);
    }
}
