package com.github.reedoverflow.stage1streader.action;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class PostNextPageAction extends AnAction {

    private ThreadPanel threadPanel;

    public PostNextPageAction(ThreadPanel threadPanel) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Next Page");
        presentation.setDescription("Post next page");
        presentation.setIcon(AllIcons.Actions.ArrowExpand);
        this.threadPanel = threadPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(threadPanel != null) {
            threadPanel.getNextPagePost();
        }
    }
}
