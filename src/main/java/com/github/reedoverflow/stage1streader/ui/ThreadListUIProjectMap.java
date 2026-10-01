package com.github.reedoverflow.stage1streader.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps one reader per project; explicit disposal removes the value-to-project reference cycle.
 */
public class ThreadListUIProjectMap {

    private static final ThreadListUIProjectMap INSTANCE = new ThreadListUIProjectMap();

    private final Map<Project, ThreadListUI> map = new WeakHashMap<>();

    private ThreadListUIProjectMap() {
    }

    public static ThreadListUIProjectMap getInstance() {
        return INSTANCE;
    }

    public synchronized ThreadListUI getThreadListUIByProject(Project project) {
        ThreadListUI threadListUI = map.get(project);
        if (threadListUI == null) {
            threadListUI = new ThreadListUI(project);
            map.put(project, threadListUI);
            Disposer.register(project, () -> {
                synchronized (ThreadListUIProjectMap.this) { map.remove(project); }
            });
        }
        return threadListUI;
    }
}
