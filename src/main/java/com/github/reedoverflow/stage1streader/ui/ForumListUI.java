package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;

import javax.swing.*;
import java.awt.*;

/**
 * @author yanzhao
 * @date 2022/1/26 13:46
 * @since 1.0.0
 */
public class ForumListUI {

    private JPanel mainPanel;

    public ForumListUI() {
        mainPanel = new ForumListPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    }

    public JComponent createComponent() {
        return mainPanel;
    }
}
