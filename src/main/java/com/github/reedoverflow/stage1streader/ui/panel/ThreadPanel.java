package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.action.PostFirstPageAction;
import com.github.reedoverflow.stage1streader.action.PostLastPageAction;
import com.github.reedoverflow.stage1streader.action.PostNextPageAction;
import com.github.reedoverflow.stage1streader.action.PostPrevPageAction;
import com.github.reedoverflow.stage1streader.action.PureTextAction;
import com.github.reedoverflow.stage1streader.action.ThreadFirstPageAction;
import com.github.reedoverflow.stage1streader.action.ThreadNextPageAction;
import com.github.reedoverflow.stage1streader.action.ThreadPrevPageAction;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Thread list and reply reader.
 */
public class ThreadPanel extends JPanel {

    private static final int DEFAULT_PAGE = 1;
    private static final int POSTS_PER_PAGE = 30;

    private static final Pattern BREAK_TAG = Pattern.compile("(?i)<br\\s*/?>|</p\\s*>");
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]*>");
    private static final Pattern EXTRA_BLANK_LINES = Pattern.compile("\\r?\\n(?:\\s*\\r?\\n)+");

    private final JBList<Thread> threadJBList = new JBList<>();
    private final JBTextArea textAreaPost = new JBTextArea();
    private final JBScrollPane threadScrollPane = new JBScrollPane(threadJBList);
    private final JBScrollPane postScrollPane = new JBScrollPane(textAreaPost);

    private final SimpleToolWindowPanel threadPanel;
    private final PureTextAction threadPageAction = new PureTextAction(String.valueOf(DEFAULT_PAGE));
    private final PureTextAction postPageAction = new PureTextAction(DEFAULT_PAGE + "/" + DEFAULT_PAGE);
    private final DiscuzService discuzService = new DiscuzService();

    private int currentThreadPage = DEFAULT_PAGE;
    private int currentPostPage = DEFAULT_PAGE;
    private int currentPostMaxPage = DEFAULT_PAGE;
    private int currentForum;
    private int currentThread;
    private boolean threadHasNextPage = true;

    private SwingWorker<List<Thread>, Void> threadWorker;
    private SwingWorker<List<Reply>, Void> postWorker;
    private long threadRequestVersion;
    private long postRequestVersion;

    public ThreadPanel() {
        super(new BorderLayout());

        configureThreadList();
        configurePostArea();

        List<AnAction> actionList = new ArrayList<>();
        actionList.add(new ThreadFirstPageAction(this));
        actionList.add(new ThreadPrevPageAction(this));
        actionList.add(threadPageAction);
        actionList.add(new ThreadNextPageAction(this));
        actionList.add(new Separator());
        actionList.add(new PostFirstPageAction(this));
        actionList.add(new PostPrevPageAction(this));
        actionList.add(postPageAction);
        actionList.add(new PostNextPageAction(this));
        actionList.add(new PostLastPageAction(this));

        threadPanel = new SimpleToolWindowPanel(true);
        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("Thread Tool Bar", new DefaultActionGroup(actionList), true);
        toolbar.setTargetComponent(this);
        threadPanel.setContent(threadScrollPane);
        threadPanel.setToolbar(toolbar.getComponent());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, threadPanel, postScrollPane);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(320);
        add(splitPane, BorderLayout.CENTER);
    }

    private void configureThreadList() {
        threadJBList.getEmptyText().setText("Select a forum to load threads");
        threadJBList.setFixedCellWidth(300);
        threadJBList.setCellRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JList list,
                                                 Object value,
                                                 int index,
                                                 boolean selected,
                                                 boolean hasFocus) {
                if (!(value instanceof Thread)) {
                    return;
                }

                Thread thread = (Thread) value;
                String subject = StringUtil.isEmptyOrSpaces(thread.getSubject())
                        ? "(No subject)"
                        : thread.getSubject();
                append(subject);
                append(" - " + calculatePostMaxPage(thread.getReplies()) + " page(s)");
            }
        });

        threadJBList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            Thread selectedThread = threadJBList.getSelectedValue();
            if (selectedThread == null) {
                return;
            }

            String threadId = selectedThread.getTid();
            if (StringUtil.isEmptyOrSpaces(threadId)) {
                invalidatePostSelection("The selected thread does not have a valid id.");
                return;
            }

            try {
                currentPostMaxPage = calculatePostMaxPage(selectedThread.getReplies());
                getPostByThreadId(Integer.parseInt(threadId.trim()));
            } catch (NumberFormatException e) {
                invalidatePostSelection("The selected thread has an invalid id: " + threadId);
            }
        });
    }

    private void configurePostArea() {
        textAreaPost.setEditable(false);
        textAreaPost.setLineWrap(true);
        textAreaPost.setWrapStyleWord(true);
        textAreaPost.setText("Select a thread to load replies.");
    }

    /**
     * Starts at page one whenever a different forum is selected.
     */
    public void getThreadByForumId(int forumId) {
        if (forumId <= 0) {
            return;
        }

        currentForum = forumId;
        currentThreadPage = DEFAULT_PAGE;
        threadHasNextPage = true;
        getAndSetThread(forumId, DEFAULT_PAGE);
    }

    private void getAndSetThread(int forumId, int page) {
        if (forumId <= 0 || page < DEFAULT_PAGE) {
            return;
        }

        if (threadWorker != null && !threadWorker.isDone()) {
            threadWorker.cancel(true);
        }

        resetPostSelection();
        final int requestPage = page;
        final long thisRequest = ++threadRequestVersion;
        currentThreadPage = requestPage;
        threadPageAction.setPage(requestPage);
        threadJBList.getEmptyText().setText("Loading threads...");
        threadJBList.clearSelection();
        threadJBList.setModel(new DefaultListModel<>());
        threadJBList.setPaintBusy(true);

        threadWorker = new SwingWorker<List<Thread>, Void>() {
            @Override
            protected List<Thread> doInBackground() {
                return discuzService.getThreadList(forumId, requestPage);
            }

            @Override
            protected void done() {
                if (thisRequest != threadRequestVersion) {
                    return;
                }

                try {
                    List<Thread> threadList = get();
                    DefaultListModel<Thread> model = new DefaultListModel<>();
                    if (threadList != null) {
                        for (Thread thread : threadList) {
                            if (thread != null) {
                                model.addElement(thread);
                            }
                        }
                    }
                    threadJBList.setModel(model);
                    threadJBList.clearSelection();
                    threadHasNextPage = !model.isEmpty();
                    threadJBList.getEmptyText().setText("No threads found on this page");
                    scrollToTop(threadScrollPane);
                } catch (CancellationException ignored) {
                    // A newer request owns the UI.
                } catch (InterruptedException e) {
                    java.lang.Thread.currentThread().interrupt();
                    showThreadError(e);
                } catch (ExecutionException e) {
                    showThreadError(e.getCause() == null ? e : e.getCause());
                } finally {
                    if (thisRequest == threadRequestVersion) {
                        threadJBList.setPaintBusy(false);
                    }
                }
            }
        };
        threadWorker.execute();
    }

    /**
     * Starts at page one whenever a different thread is selected.
     */
    public void getPostByThreadId(int threadId) {
        if (threadId <= 0) {
            return;
        }
        currentPostPage = DEFAULT_PAGE;
        currentThread = threadId;
        getAndSetPost(threadId, DEFAULT_PAGE);
    }

    private void getAndSetPost(int threadId, int page) {
        if (threadId <= 0) {
            return;
        }

        int targetPage = Math.max(DEFAULT_PAGE, Math.min(page, currentPostMaxPage));
        if (postWorker != null && !postWorker.isDone()) {
            postWorker.cancel(true);
        }

        final long thisRequest = ++postRequestVersion;
        currentPostPage = targetPage;
        postPageAction.setPage(targetPage, currentPostMaxPage);
        textAreaPost.setText("Loading replies...");
        textAreaPost.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        postWorker = new SwingWorker<List<Reply>, Void>() {
            @Override
            protected List<Reply> doInBackground() {
                return discuzService.getReplyList(threadId, targetPage);
            }

            @Override
            protected void done() {
                if (thisRequest != postRequestVersion) {
                    return;
                }

                try {
                    List<Reply> replies = get();
                    textAreaPost.setText(renderReplies(replies));
                    textAreaPost.setCaretPosition(0);
                    scrollToTop(postScrollPane);
                } catch (CancellationException ignored) {
                    // A newer request owns the UI.
                } catch (InterruptedException e) {
                    java.lang.Thread.currentThread().interrupt();
                    showPostError(readableMessage(e));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showPostError(readableMessage(cause));
                } finally {
                    if (thisRequest == postRequestVersion) {
                        textAreaPost.setCursor(Cursor.getDefaultCursor());
                        postPageAction.setPage(currentPostPage, currentPostMaxPage);
                    }
                }
            }
        };
        postWorker.execute();
    }

    private String renderReplies(List<Reply> replies) {
        if (replies == null || replies.isEmpty()) {
            return "No replies found on this page.";
        }

        StringBuilder content = new StringBuilder();
        for (Reply reply : replies) {
            if (reply == null) {
                continue;
            }
            content.append("----------------------------\n")
                    .append(defaultText(reply.getAuthor(), "Anonymous"))
                    .append("    ")
                    .append(defaultText(reply.getDateline(), ""))
                    .append("    ")
                    .append(defaultText(reply.getPosition(), "?"))
                    .append("L\n")
                    .append(toPlainText(reply.getMessage()))
                    .append('\n');
        }
        return content.length() == 0 ? "No replies found on this page." : content.toString();
    }

    private String toPlainText(String html) {
        if (html == null) {
            return "";
        }

        String text = BREAK_TAG.matcher(html).replaceAll("\n");
        text = HTML_TAG.matcher(text).replaceAll("");
        text = StringUtil.unescapeXmlEntities(text.replace("&nbsp;", " "));
        return EXTRA_BLANK_LINES.matcher(text).replaceAll("\n").trim();
    }

    private static int calculatePostMaxPage(String repliesValue) {
        long replyCount = 0;
        try {
            if (!StringUtil.isEmptyOrSpaces(repliesValue)) {
                replyCount = Math.max(0, Long.parseLong(repliesValue));
            }
        } catch (NumberFormatException ignored) {
            return DEFAULT_PAGE;
        }

        long totalPosts = replyCount == Long.MAX_VALUE ? Long.MAX_VALUE : replyCount + 1;
        long pageCount = totalPosts / POSTS_PER_PAGE;
        if (totalPosts % POSTS_PER_PAGE != 0) {
            pageCount++;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(DEFAULT_PAGE, pageCount));
    }

    private void showThreadError(Throwable error) {
        threadHasNextPage = false;
        threadJBList.setModel(new DefaultListModel<>());
        threadJBList.getEmptyText().setText("Load failed: " + readableMessage(error));
    }

    private void showPostError(String message) {
        textAreaPost.setText("Load failed: " + message);
        textAreaPost.setCaretPosition(0);
    }

    private void invalidatePostSelection(String message) {
        cancelPostRequest();
        currentThread = 0;
        currentPostPage = DEFAULT_PAGE;
        currentPostMaxPage = DEFAULT_PAGE;
        postPageAction.setPage(DEFAULT_PAGE, DEFAULT_PAGE);
        showPostError(message);
    }

    private void cancelPostRequest() {
        ++postRequestVersion;
        if (postWorker != null && !postWorker.isDone()) {
            postWorker.cancel(true);
        }
        textAreaPost.setCursor(Cursor.getDefaultCursor());
    }

    private void resetPostSelection() {
        cancelPostRequest();
        currentThread = 0;
        currentPostPage = DEFAULT_PAGE;
        currentPostMaxPage = DEFAULT_PAGE;
        postPageAction.setPage(DEFAULT_PAGE, DEFAULT_PAGE);
        textAreaPost.setText("Select a thread to load replies.");
    }

    private boolean isThreadLoading() {
        return threadWorker != null && !threadWorker.isDone();
    }

    private boolean isPostLoading() {
        return postWorker != null && !postWorker.isDone();
    }

    private static void scrollToTop(JScrollPane scrollPane) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String readableMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    // Thread-list pagination.
    public void getFirstPageThread() {
        if (currentForum != 0 && !isThreadLoading()) {
            threadHasNextPage = true;
            getAndSetThread(currentForum, DEFAULT_PAGE);
        }
    }

    public void getNextPageThread() {
        if (currentForum != 0
                && currentThreadPage < Integer.MAX_VALUE
                && threadHasNextPage
                && !isThreadLoading()) {
            getAndSetThread(currentForum, currentThreadPage + 1);
        }
    }

    public void getPrevPageThread() {
        if (currentForum != 0 && currentThreadPage > DEFAULT_PAGE && !isThreadLoading()) {
            threadHasNextPage = true;
            getAndSetThread(currentForum, currentThreadPage - 1);
        }
    }

    // Reply pagination.
    public void getFirstPagePost() {
        if (currentThread != 0 && !isPostLoading()) {
            getAndSetPost(currentThread, DEFAULT_PAGE);
        }
    }

    public void getLastPagePost() {
        if (currentThread != 0 && !isPostLoading()) {
            getAndSetPost(currentThread, currentPostMaxPage);
        }
    }

    public void getNextPagePost() {
        if (currentThread != 0 && currentPostPage < currentPostMaxPage && !isPostLoading()) {
            getAndSetPost(currentThread, currentPostPage + 1);
        }
    }

    public void getPrevPagePost() {
        if (currentThread != 0 && currentPostPage > DEFAULT_PAGE && !isPostLoading()) {
            getAndSetPost(currentThread, currentPostPage - 1);
        }
    }
}
