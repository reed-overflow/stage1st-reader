package com.github.reedoverflow.stage1streader.ui;

import javax.swing.*;

/**
 * @author yanzhao
 * @date 2022/1/26 13:46
 * @since 1.0.0
 */
public class WindowUI {

    private JPanel mainPanel;

    public WindowUI() {
        mainPanel = new MainPanel();
    }

    public JComponent createComponent() {
        return mainPanel;
    }
}
