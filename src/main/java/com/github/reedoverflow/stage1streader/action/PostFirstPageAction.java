package com.github.reedoverflow.stage1streader.action;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class PostFirstPageAction extends AnAction {

    private ThreadPanel threadPanel;

    public PostFirstPageAction(ThreadPanel threadPanel) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText("First Page");
        presentation.setDescription("Post first page");
        presentation.setIcon(AllIcons.Actions.Play_first);
        this.threadPanel = threadPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(threadPanel != null) {
            threadPanel.getFirstPagePost();
        }
    }
}
