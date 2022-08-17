package com.github.reedoverflow.stage1streader.window;

import com.github.reedoverflow.stage1streader.ui.WindowUI;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class WindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final WindowUI windowUI = new WindowUI();
        final ContentFactory instance = ContentFactory.SERVICE.getInstance();
        final Content content = instance.createContent(windowUI.createComponent(), "Saraba", false);
        toolWindow.getContentManager().addContent(content);
    }
}
