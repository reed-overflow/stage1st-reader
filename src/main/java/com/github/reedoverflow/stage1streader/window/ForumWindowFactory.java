package com.github.reedoverflow.stage1streader.window;

import com.github.reedoverflow.stage1streader.ui.ForumListUI;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ForumWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final ForumListUI forumListUI = ForumListUI.getInstance();
        final ContentFactory instance = ContentFactory.SERVICE.getInstance();
        final Content content = instance.createContent(forumListUI.createComponent(), "Obey or Die", false);
        toolWindow.getContentManager().addContent(content);
    }
}
