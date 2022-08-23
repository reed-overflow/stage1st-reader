package com.github.reedoverflow.stage1streader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PureTextAction extends AnAction implements CustomComponentAction {
    private JPanel panel;
    private String text;

    private JLabel show;

    public PureTextAction(String text) {
        panel = new JPanel();
        this.text = text;
        show = new JLabel(text);
        panel.add(show);
    }

    public void setPage(int page) {
        show.setText(page+"");
    }

    public void setPage(int page,int maxPage) {
        show.setText(page+"/"+maxPage);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    @Override
    public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        return panel;
    }
}
