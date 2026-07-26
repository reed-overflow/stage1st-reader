package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.github.reedoverflow.stage1streader.ui.ThreadListUI;
import com.github.reedoverflow.stage1streader.ui.ThreadListUIProjectMap;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Forum browser tree.
 */
public class ForumListPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(ForumListPanel.class);

    private final Project project;
    private final DiscuzService discuzService = new DiscuzService();
    private final DefaultMutableTreeNode top = new DefaultMutableTreeNode("Forum list");
    private final Tree tree = new Tree(top);

    private SwingWorker<List<Forum>, Void> forumWorker;
    private long requestVersion;

    public ForumListPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        configureTree();
        add(new JBScrollPane(tree), BorderLayout.CENTER);
        createForumTree();
    }

    private void configureTree() {
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree,
                                              Object value,
                                              boolean selected,
                                              boolean expanded,
                                              boolean leaf,
                                              int row,
                                              boolean hasFocus) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof Forum) {
                    Forum forum = (Forum) userObject;
                    setIcon(hasChildren(forum) ? AllIcons.Nodes.Folder : AllIcons.FileTypes.Text);
                    append(StringUtil.notNullize(forum.getName(), "Unnamed forum"));
                } else {
                    setIcon(null);
                    append(String.valueOf(userObject));
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || project.isDisposed()) {
                    return;
                }

                TreePath path = tree.getPathForLocation(event.getX(), event.getY());
                if (path == null) {
                    return;
                }
                tree.setSelectionPath(path);

                Object component = path.getLastPathComponent();
                if (!(component instanceof DefaultMutableTreeNode)) {
                    return;
                }
                Object nodeInfo = ((DefaultMutableTreeNode) component).getUserObject();
                if (!(nodeInfo instanceof Forum)) {
                    return;
                }

                String forumId = ((Forum) nodeInfo).getFid();
                if (StringUtil.isEmptyOrSpaces(forumId)) {
                    return;
                }

                try {
                    ThreadListUIProjectMap map = ThreadListUIProjectMap.getInstance();
                    ThreadListUI threadListUI = map.getThreadListUIByProject(project);
                    threadListUI.getThreadByForumId(Integer.parseInt(forumId.trim()));
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid forum id returned by the forum: " + forumId, e);
                }
            }
        });
    }

    /**
     * Reloads the existing tree model.  Reusing the tree is important because
     * the component already belongs to the tool-window hierarchy.
     */
    public void createForumTree() {
        if (forumWorker != null && !forumWorker.isDone()) {
            forumWorker.cancel(true);
        }

        final long thisRequest = ++requestVersion;
        showLoadingState();

        forumWorker = new SwingWorker<List<Forum>, Void>() {
            @Override
            protected List<Forum> doInBackground() {
                return discuzService.getForumList();
            }

            @Override
            protected void done() {
                if (thisRequest != requestVersion) {
                    return;
                }

                try {
                    List<Forum> forums = get();
                    showForums(forums == null ? Collections.emptyList() : forums);
                } catch (CancellationException ignored) {
                    // A newer refresh request owns the UI now.
                } catch (InterruptedException e) {
                    java.lang.Thread.currentThread().interrupt();
                    showError(e);
                } catch (ExecutionException e) {
                    showError(e.getCause() == null ? e : e.getCause());
                } finally {
                    if (thisRequest == requestVersion) {
                        tree.setPaintBusy(false);
                    }
                }
            }
        };
        forumWorker.execute();
    }

    private void showLoadingState() {
        top.removeAllChildren();
        top.setUserObject("Loading forums...");
        reloadTree();
        tree.setPaintBusy(true);
    }

    private void showForums(List<Forum> forumList) {
        top.removeAllChildren();
        top.setUserObject("Forum list");
        boolean hasForum = false;
        for (Forum forum : forumList) {
            if (forum == null) {
                continue;
            }
            hasForum = true;
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(forum);
            setUpChildren(node, forum);
            top.add(node);
        }
        if (!hasForum) {
            top.add(new DefaultMutableTreeNode("No forums found"));
        }
        reloadTree();
        tree.expandRow(0);
    }

    private void showError(Throwable error) {
        String message = readableMessage(error);
        LOG.warn("Unable to load forum list", error);

        top.removeAllChildren();
        top.setUserObject("Forum list");
        top.add(new DefaultMutableTreeNode("Load failed: " + message));
        reloadTree();
        tree.expandRow(0);
    }

    private void reloadTree() {
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    private void setUpChildren(DefaultMutableTreeNode node, Forum forum) {
        if (!hasChildren(forum)) {
            return;
        }
        for (Forum subForum : forum.getSublist()) {
            if (subForum == null) {
                continue;
            }
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subForum);
            node.add(subNode);
            setUpChildren(subNode, subForum);
        }
    }

    private static boolean hasChildren(Forum forum) {
        return forum.getSublist() != null && !forum.getSublist().isEmpty();
    }

    private String readableMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return StringUtil.notNullize(current.getMessage(), current.getClass().getSimpleName());
    }
}
