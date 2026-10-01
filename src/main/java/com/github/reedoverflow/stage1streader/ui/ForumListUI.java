package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.action.ForumRefreshAction;
import com.github.reedoverflow.stage1streader.action.HideReaderAction;
import com.github.reedoverflow.stage1streader.service.SessionManager;
import com.github.reedoverflow.stage1streader.settings.Stage1stReaderConfigurable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yanzhao
 * @date 2022/1/26 13:46
 * @since 1.0.0
 */
public class ForumListUI {

    private final SimpleToolWindowPanel toolbarPanel;

    public ForumListUI(Project project) {
        // 板块列表panel
        ForumListPanel forumListPanel = new ForumListPanel(project);
        // 工具栏action
        List<AnAction> actionList = new ArrayList<>();
        actionList.add(new ForumRefreshAction(forumListPanel));
        actionList.add(new AnAction("登录 / 切换账号","登录/切换账号", AllIcons.Debugger.SmartStepInto) {
            @Override public void actionPerformed(AnActionEvent e) { new AccountDialog(project).show(); }
        });
        actionList.add(new AnAction("退出登录", "清除当前登录及设置中保存的 Cookie", AllIcons.Actions.StepOut) {
            @Override public void actionPerformed(AnActionEvent e) { SessionManager.getInstance().logout(); }
        });
        actionList.add(new AnAction("设置","设置",AllIcons.Debugger.VariablesTab) {
            @Override public void actionPerformed(AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, Stage1stReaderConfigurable.class);
            }
        });
        actionList.add(new HideReaderAction());
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup(actionList);
        // 设置工具栏
        toolbarPanel = new SimpleToolWindowPanel(true);
        ActionToolbar toolBar = ActionManager.getInstance().createActionToolbar("Forum Tool Bar", defaultActionGroup, true);
        toolBar.setTargetComponent(toolbarPanel.getComponent());
        toolbarPanel.setContent(forumListPanel);
        toolbarPanel.setToolbar(toolBar.getComponent());
    }

    public JComponent createComponent() {
        return toolbarPanel;
    }
}
