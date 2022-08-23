package com.github.reedoverflow.stage1streader.action;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class PostLastPageAction extends AnAction {

    private ThreadPanel threadPanel;

    public PostLastPageAction(ThreadPanel threadPanel) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Last Page");
        presentation.setDescription("Post last page");
        presentation.setIcon(AllIcons.Actions.Play_last);
        this.threadPanel = threadPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(threadPanel != null) {
            threadPanel.getLastPagePost();
        }
    }
}
