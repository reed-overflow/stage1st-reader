package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.domain.Forum;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.github.reedoverflow.stage1streader.ui.ThreadListUI;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * 帖子
 */
public class ThreadPanel extends JPanel {

    private JBList threadJBList = new JBList();
    private JBScrollPane threadScrollPane = new JBScrollPane(threadJBList);

    private JBTextArea textAreaPost = new JBTextArea();
    private JBScrollPane postScrollPane = new JBScrollPane(textAreaPost);

    private final int DEFAULT_PAGE = 1;
    private int currentThreadPage = 1;
    private int currentPostPage = 1;

    public ThreadPanel() {
        super();
        this.add(threadScrollPane);
        this.add(postScrollPane);

        // 帖子列表布局
        threadJBList.setCellRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if(value instanceof Thread) {
                    append(((Thread) value).getSubject());
                }
            }
        });
        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Object object = threadJBList.getSelectedValue();
                if(object != null && object instanceof Thread) {
                    String tid = ((Thread) object).getTid();
                    if(StringUtil.isNotEmpty(tid)) {
                        getPostByThreadId(Integer.parseInt(tid));
                    }
                }
            }
        };
        threadJBList.addMouseListener(ml);
        // 页面布局
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.addLayoutComponent(threadScrollPane, BorderLayout.WEST);
        borderLayout.addLayoutComponent(postScrollPane, BorderLayout.CENTER);
        this.setLayout(borderLayout);
    }

    public void getThreadByForumId(int forumId) {
        currentThreadPage = DEFAULT_PAGE;
        currentPostPage = DEFAULT_PAGE;
        threadJBList.setPaintBusy(true);
        DiscuzService discuzService = new DiscuzService();
        List<Thread> threadList = discuzService.getThreadList(Integer.valueOf(forumId), currentThreadPage);

        DefaultListModel<Thread> defaultListModel = new DefaultListModel<>();
        for (Thread thread: threadList
             ) {
            defaultListModel.addElement(thread);
        }
        threadJBList.setModel(defaultListModel);

        threadJBList.setPaintBusy(false);

    }

    public void getPostByThreadId(int threadId) {
        currentPostPage = DEFAULT_PAGE;
        DiscuzService discuzService = new DiscuzService();
        List<Reply> replyList = discuzService.getReplyList(threadId, currentPostPage);

        StringBuilder stringBuilder = new StringBuilder();
        for (Reply reply: replyList
             ) {
            stringBuilder.append("----------------------------\n");
            stringBuilder.append(reply.getAuthor()).append("    ").append(reply.getDateline()).append("\n");
            stringBuilder.append(reply.getMessage()).append("\n");
        }
        textAreaPost.setText(stringBuilder.toString());
    }
}
