package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.github.reedoverflow.stage1streader.ui.ThreadListUI;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.treeStructure.Tree;
import jnr.ffi.annotations.In;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * 板块
 */
public class ForumListPanel extends JPanel {

    private Tree tree = new Tree();

    public ForumListPanel() {
        super();
        tree.setPaintBusy(true);
        // 根节点
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Forum list");
        createNodes(top);
        tree = new Tree(top);
        tree.setPaintBusy(false);
        // tree渲染
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof Forum) {
                    if(((Forum) userObject).getSublist() == null || ((Forum) userObject).getSublist().size() == 0) {
                        // 子节点
                        setIcon(AllIcons.FileTypes.Text);
                    }else {
                        // 根节点
                        setIcon(AllIcons.Nodes.Folder);
                    }
                    append(((Forum) userObject).getName());
                } else {
                    setIcon(null);
                    append(value.toString());
                }
            }
        });
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
//                        Messages.showMessageDialog(forum.getDescription(), forum.getName(), Messages.getInformationIcon());
                        String forumId = forum.getFid();
                        if(StringUtil.isNotEmpty(forumId)) {
                            ThreadListUI threadListUI = ThreadListUI.getInstance();
                            threadListUI.getThreadByForumId(Integer.parseInt(forumId));
                        }

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

    /**
     * 字节点设置
     * @param node
     * @param forum
     */
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

}
