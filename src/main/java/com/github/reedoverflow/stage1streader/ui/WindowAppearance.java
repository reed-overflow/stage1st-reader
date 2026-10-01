package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public final class WindowAppearance {
    private WindowAppearance() { }
    public static void apply(Project project) {
        if (project.isDisposed()) return;
        boolean camouflage = Config.getInstance().isCamouflage();
        update(project, "S1 Selector", camouflage ? "Index" : "S1 Selector");
        update(project, "S1 Reader", camouflage ? "Output" : "S1 Reader");
    }
    private static void update(Project project, String id, String title) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(id);
        if (window == null) return;
        window.setTitle(title);
        window.setStripeTitle(title);
        // Do not call getContentManager here: it lazily creates the other window's factory.
        // That can re-enter ThreadListUIProjectMap while its first reader is still constructing.
    }
}
