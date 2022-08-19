package com.github.reedoverflow.stage1streader.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author yanzhao
 * @date 2022/1/26 13:46
 * @since 1.0.0
 */
public class WindowUI {

    private JPanel mainPanel;

    public WindowUI() {
        mainPanel = new MainPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    }

    public JComponent createComponent() {
        return mainPanel;
    }
}
