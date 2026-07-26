package com.github.reedoverflow.stage1streader.ui;

import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps one thread reader per open project without retaining closed projects.
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
            threadListUI = new ThreadListUI();
            map.put(project, threadListUI);
        }
        return threadListUI;
    }
}
