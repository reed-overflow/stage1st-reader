package com.github.reedoverflow.stage1streader.window;

import com.github.reedoverflow.stage1streader.ui.ThreadListUI;
import com.github.reedoverflow.stage1streader.ui.ThreadListUIProjectMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ThreadWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ThreadListUIProjectMap map = ThreadListUIProjectMap.getInstance();
        ThreadListUI threadListUI = map.getThreadListUIByProject(project);
        ContentFactory instance = ContentFactory.SERVICE.getInstance();
        Content content = instance.createContent(threadListUI.createComponent(), "Output", false);
        toolWindow.getContentManager().addContent(content);
        com.github.reedoverflow.stage1streader.ui.WindowAppearance.apply(project);
    }
}
