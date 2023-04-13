package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.ui.panel.ThreadPanel;
import com.intellij.openapi.project.Project;
import cucumber.api.java.it.Ma;

import java.util.HashMap;
import java.util.Map;

public class ThreadListUIProjectMap {
    private static ThreadListUIProjectMap instance;

    private Map<Integer, ThreadListUI> map;

    static {
        instance = new ThreadListUIProjectMap();
    }

    private ThreadListUIProjectMap() {
        map = new HashMap<>();
    }

    public static ThreadListUIProjectMap getInstance() {
        return instance;
    }

    public ThreadListUI getThreadListUIByProject(Project project) {
        Integer code = project.hashCode();
        ThreadListUI threadListUI = map.get(code);
        if(threadListUI == null) {
            threadListUI = new ThreadListUI();
            map.put(code, threadListUI);
        }
        return threadListUI;
    }
}
