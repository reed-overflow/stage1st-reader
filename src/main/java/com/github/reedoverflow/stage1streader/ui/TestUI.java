package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yanzhao
 * @date 2022/1/26 13:46
 * @since 1.0.0
 */
public class TestUI implements Configurable {

    private JPanel mainPanel;

    public TestUI() {
        mainPanel = new ForumListPanel();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "displayName";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
