package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;
import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;

import javax.swing.*;
import java.awt.*;

public class ThreadListUI {
    private JPanel mainPanel;

    public ThreadListUI() {
        mainPanel = new ThreadPanel();
//        mainPanel.setLayout(new BorderLayout());
//        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    public JComponent createComponent() {
        return mainPanel;
    }
}
