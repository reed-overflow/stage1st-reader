package com.github.reedoverflow.stage1streader.action;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class PostPrevPageAction extends AnAction {

    private ThreadPanel threadPanel;

    public PostPrevPageAction(ThreadPanel threadPanel) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Previous Page");
        presentation.setDescription("Post previous page");
        presentation.setIcon(AllIcons.Actions.ArrowCollapse);
        this.threadPanel = threadPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(threadPanel != null) {
            threadPanel.getPrevPagePost();
        }
    }
}
