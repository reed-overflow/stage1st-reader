package com.github.reedoverflow.stage1streader.action;

import com.github.reedoverflow.stage1streader.ui.panel.ForumListPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;

/**
 * 右键文件内容时触发
 *
 * @author yanzhao
 * @date 2022/1/26 13:44
 * @since 1.0.0
 */
public class ForumRefreshAction extends AnAction {

    private ForumListPanel forumListPanel;

    public ForumRefreshAction(ForumListPanel forumListPanel) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Refresh");
        presentation.setDescription("Refresh forum list");
        presentation.setIcon(AllIcons.Actions.Refresh);
        this.forumListPanel = forumListPanel;
    }

    /**
     * 点击触发方法
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        forumListPanel.createForumTree();
    }

}
