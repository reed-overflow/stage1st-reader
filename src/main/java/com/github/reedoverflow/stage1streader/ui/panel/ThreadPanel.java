package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.action.*;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 帖子
 */
public class ThreadPanel extends JPanel {

    // 帖子列表
    private JBList threadJBList = new JBList();
    // 回复列表
    private JBTextArea textAreaPost = new JBTextArea();
    // 滚动条
    private JBScrollPane threadScrollPane = new JBScrollPane(threadJBList);
    private JBScrollPane postScrollPane = new JBScrollPane(textAreaPost);

    // 分页工具栏
    private SimpleToolWindowPanel threadPanel;
    // 页码
    private final int DEFAULT_PAGE = 1;
    private int currentThreadPage = DEFAULT_PAGE;
    private int currentPostPage = DEFAULT_PAGE;
    private int currentPostMaxPage = DEFAULT_PAGE;

    private int currentForum = 0;
    private int currentThread = 0;
    // 页码显示
    private PureTextAction threadPageAction = new PureTextAction(currentThreadPage+"");
    private PureTextAction postPageAction = new PureTextAction(currentPostPage+"/"+currentPostMaxPage);

    private DiscuzService discuzService = new DiscuzService();

    public ThreadPanel() {
        super();

        // 帖子列表布局
        threadJBList.setCellRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if(value instanceof Thread) {
                    int totalPage = (int) Math.ceil(Double.parseDouble(((Thread) value).getReplies()) / 30);
                    append(((Thread) value).getSubject()).append(" - ").append(totalPage+"");
                }
            }
        });
        threadJBList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Object object = threadJBList.getSelectedValue();
                if(object instanceof Thread) {
                    String tid = ((Thread) object).getTid();
                    int totalPage = (int) Math.ceil(Double.parseDouble(((Thread) object).getReplies()) / 30);
                    currentPostMaxPage = totalPage;
                    if(StringUtil.isNotEmpty(tid)) {
                        getPostByThreadId(Integer.parseInt(tid));
                    }
                }
            }
        });

        // 工具栏action
        List<AnAction> actionList = new ArrayList<>();
        // 帖子工具栏
        actionList.add(new ThreadFirstPageAction(this));
        actionList.add(new ThreadPrevPageAction(this));
        actionList.add(threadPageAction);
        actionList.add(new ThreadNextPageAction(this));
        actionList.add(new Separator());
        // 回复工具栏
        actionList.add(new PostFirstPageAction(this));
        actionList.add(new PostPrevPageAction(this));
        actionList.add(postPageAction);
        actionList.add(new PostNextPageAction(this));
        actionList.add(new PostLastPageAction(this));
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup(actionList);
        // 设置工具栏
        threadPanel = new SimpleToolWindowPanel(true);
        threadJBList.setFixedCellWidth(100);
        ActionToolbar threadPageToolbar = ActionManager.getInstance() .createActionToolbar("Thread Tool Bar", defaultActionGroup, true);
        threadPageToolbar.setTargetComponent(threadPanel.getComponent());
        threadPanel.setContent(threadScrollPane);
        threadPanel.setToolbar(threadPageToolbar.getComponent());

        this.add(threadPanel);
        this.add(postScrollPane);

        // 页面布局
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.addLayoutComponent(threadPanel, BorderLayout.WEST);
        borderLayout.addLayoutComponent(postScrollPane, BorderLayout.CENTER);
        this.setLayout(borderLayout);
    }

    /**
     * 帖子列表初始化
     * @param forumId
     */
    public void getThreadByForumId(int forumId) {
        currentThreadPage = DEFAULT_PAGE;
        currentPostPage = DEFAULT_PAGE;
        currentForum = forumId;
        getAndSetThread(forumId, currentThreadPage);
    }

    /**
     * 获取并显示帖子列表
     * @param forumId
     * @param page
     */
    private void getAndSetThread(int forumId, int page) {
        threadJBList.setPaintBusy(true);
        // 滚动条回到顶部
        threadScrollPane.setViewportView(threadJBList);
        threadScrollPane.getVerticalScrollBar().setValue(0);
        threadPageAction.setPage(page);

        SwingWorker threadWorker = new SwingWorker() {
            List<Thread> threadList = new ArrayList<>();
            @Override
            protected Object doInBackground() throws Exception {
                // 获取列表
                threadList = discuzService.getThreadList(forumId, page);
                return threadList;
            }

            @Override
            protected void done() {
                super.done();
                // 封装渲染
                DefaultListModel<Thread> defaultListModel = new DefaultListModel<>();
                for (Thread thread: threadList
                ) {
                    defaultListModel.addElement(thread);
                }
                threadJBList.setModel(defaultListModel);

                threadJBList.setPaintBusy(false);
                // 滚动条回到顶部
                threadScrollPane.setViewportView(threadJBList);
                threadScrollPane.getVerticalScrollBar().setValue(0);
                threadPageAction.setPage(page);
            }
        };
        threadWorker.execute();
    }

    /**
     * 回复列表初始化
     * @param threadId
     */
    public void getPostByThreadId(int threadId) {
        currentPostPage = DEFAULT_PAGE;
        currentThread = threadId;
        getAndSetPost(currentThread, currentPostPage);
    }

    /**
     * 获取并显示回复列表
     * @param tId
     * @param page
     */
    private void getAndSetPost(int tId, int page) {
        //文字自动换行
        textAreaPost.setLineWrap(Boolean.TRUE);
        //字体设置为主界面字体
        textAreaPost.setFont(super.getFont());
        // 滚动条回到顶部
        textAreaPost.setSelectionStart(0);
        textAreaPost.setSelectionEnd(0);
        postScrollPane.getVerticalScrollBar().setValue(postScrollPane.getVerticalScrollBar().getMaximum());
        postPageAction.setPage(page,+currentPostMaxPage);
        SwingWorker postWorker = new SwingWorker() {
            List<Reply> replyList = new ArrayList<>();
            @Override
            protected Object doInBackground() throws Exception {
                // 获取列表
                DiscuzService discuzService = new DiscuzService();
                replyList = discuzService.getReplyList(tId, currentPostPage);
                return replyList;
            }

            @Override
            protected void done() {
                super.done();
                // 封装渲染
                StringBuilder stringBuilder = new StringBuilder();

                for (Reply reply: replyList
                ) {
                    stringBuilder.append("----------------------------\n");
                    stringBuilder.append(reply.getAuthor())
                            .append("    ")
                            .append(reply.getDateline())
                            .append("    ")
                            .append(reply.getPosition())
                            .append("L")
                            .append("\n");
                    String message = reply.getMessage();
                    //去除所有 html 标签，包括表情图片链接。目前没啥好方法显示表情所以去除
                    message = message.replaceAll("<.*?>", "");
                    //将多个换行符合并成一个
                    message = message.replaceAll("(\r?\n(\\s*\r?\n)+)","\n");
                    stringBuilder.append(message).append("\n");
                }
                textAreaPost.setText(stringBuilder.toString());
                // 滚动条回到顶部
                textAreaPost.setSelectionStart(0);
                textAreaPost.setSelectionEnd(0);
                postScrollPane.getVerticalScrollBar().setValue(postScrollPane.getVerticalScrollBar().getMaximum());
                postPageAction.setPage(page,+currentPostMaxPage);
            }
        };
        postWorker.execute();
    }
    // 帖子列表页码控制
    public void getFirstPageThread() {
        if(currentForum != 0) {
            currentThreadPage = 1;
            getAndSetThread(currentForum,currentThreadPage);
        }
    }

    public void getNextPageThread() {
        if(currentForum != 0) {
            currentThreadPage = currentThreadPage + 1;
            getAndSetThread(currentForum,currentThreadPage);
        }
    }

    public void getPrevPageThread() {
        if(currentForum != 0 && currentThreadPage > 1) {
            currentThreadPage = currentThreadPage - 1;
            getAndSetThread(currentForum,currentThreadPage);
        }
    }
    // 回复页码控制
    public void getFirstPagePost() {
        if(currentThread != 0) {
            currentPostPage = 1;
            getAndSetPost(currentThread,currentPostPage);
        }
    }

    public void getLastPagePost() {
        if(currentThread != 0) {
            currentPostPage = currentPostMaxPage;
            getAndSetPost(currentThread,currentPostPage);
        }
    }

    public void getNextPagePost() {
        if(currentThread != 0) {
            currentPostPage = currentPostPage + 1;
            getAndSetPost(currentThread,currentPostPage);
        }
    }

    public void getPrevPagePost() {
        if(currentThread != 0) {
            currentPostPage = currentPostPage - 1;
            getAndSetPost(currentThread,currentPostPage);
        }
    }
}
