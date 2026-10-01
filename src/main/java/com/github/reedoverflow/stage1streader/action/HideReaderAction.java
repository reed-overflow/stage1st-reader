package com.github.reedoverflow.stage1streader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import static com.intellij.icons.AllIcons.General.HideToolWindow;

public final class HideReaderAction extends AnAction {
    public HideReaderAction() { super("隐藏阅读窗口", "隐藏阅读窗口", HideToolWindow); }
    @Override public void actionPerformed(AnActionEvent e) { hide(e.getProject()); }
    public static void hide(Project project) {
        if (project == null || project.isDisposed()) return;
        for (String id : new String[]{"S1 Reader", "S1 Selector"}) {
            ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(id);
            if (window != null) window.hide(null);
        }
    }
}
