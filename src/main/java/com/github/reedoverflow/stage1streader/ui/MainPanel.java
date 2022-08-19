package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import com.intellij.ui.PanelWithButtons;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * @author yanzhao
 * @date 2022/1/26 14:47
 * @since 1.0.0
 */
public class MainPanel extends JPanel {

    public MainPanel() {
        super();
        // 根节点
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode("Forum list");
        createNodes(top);
        Tree tree = new Tree(top);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new MyTreeCellRenderer());
        // 节点监听
        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if(e.getClickCount() == 1) {
                    }
                    else if(e.getClickCount() == 2) {
                        // 双击
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if (node == null) {
                            return;
                        }
                        Object nodeInfo = node.getUserObject();
                        Forum forum = (Forum) nodeInfo;
                        Messages.showMessageDialog(forum.getDescription(), forum.getName(), Messages.getInformationIcon());
                    }
                }
            }
        };
        tree.addMouseListener(ml);
        this.add(tree);
    }

    /**
     * treeNode 节点设置
     * @param top
     */
    private void createNodes(DefaultMutableTreeNode top) {
        DiscuzService discuzService = new DiscuzService();
        List<Forum> forumList = discuzService.getForumList();

        DefaultMutableTreeNode node = null;
        for (Forum forum: forumList
             ) {
            node = new DefaultMutableTreeNode(forum);
            setUpChildren(node, forum);
            top.add(node);
        }
    }

    private void setUpChildren(DefaultMutableTreeNode node, Forum forum) {
        if(forum.getSublist() != null && forum.getSublist().size() > 0) {
            for (Forum subForum: forum.getSublist()
                 ) {
                DefaultMutableTreeNode sub = new DefaultMutableTreeNode(subForum);
                node.add(sub);
                setUpChildren(sub, subForum);
            }
        }
    }

    /**
     * 自定义tree渲染方式
     */
    private static class MyTreeCellRenderer implements TreeCellRenderer {
        private final JLabel label;

        MyTreeCellRenderer() {
            label = new JLabel();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof Forum) {

                if(((Forum) userObject).getSublist() == null || ((Forum) userObject).getSublist().size() == 0) {
                    // 子节点
                    label.setIcon(AllIcons.FileTypes.Text);
                }else {
                    // 根节点
                    label.setIcon(AllIcons.Nodes.Folder);
                }
                label.setText(((Forum) userObject).getName());
            } else {
                label.setIcon(null);
                label.setText(value.toString());
            }
            return label;
        }
    }
}
