package com.github.reedoverflow.stage1streader.ui.panel;

import com.github.reedoverflow.stage1streader.action.*;
import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.domain.ForumPage;
import com.github.reedoverflow.stage1streader.domain.Reply;
import com.github.reedoverflow.stage1streader.domain.Thread;
import com.github.reedoverflow.stage1streader.service.*;
import com.github.reedoverflow.stage1streader.ui.AccountDialog;
import com.github.reedoverflow.stage1streader.ui.ComposeDialog;
import com.github.reedoverflow.stage1streader.utils.PostText;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/** All network work is off the EDT. Results are bound to both request and account generations. */
public class ThreadPanel extends JPanel implements Disposable {
    private final Project project;
    private final JBList<Thread> threadList = new JBList<>();
    private final JBTextArea postText = new JBTextArea();
    private final JTextField filter = new JTextField();
    private final JLabel status = new JLabel("选择版块开始浏览");
    private final PureTextAction threadPageAction = new PureTextAction("1/1");
    private final PureTextAction postPageAction = new PureTextAction("1/1");
    private List<Thread> threads = Collections.emptyList();
    private List<Reply> posts = Collections.emptyList();
    private final List<Integer> postOffsets = new ArrayList<>();
    private Map<String, String> types = Collections.emptyMap();
    private boolean typeRequired;
    private boolean forumLoaded;
    private int currentForum;
    private int currentThread;
    private int replyForum;
    private int threadPage = 1;
    private int threadMax = 1;
    private int postPage = 1;
    private int postMax = 1;
    private String subject = "";
    private long threadVersion;
    private long postVersion;
    private boolean disposed;
    private SwingWorker<ForumPage<Thread>, Void> threadWorker;
    private SwingWorker<ForumPage<Reply>, Void> postWorker;

    public ThreadPanel(Project project) {
        super(new BorderLayout(4, 4));
        this.project = project;
        Disposer.register(project, this);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(ReaderEvents.TOPIC,
                reset -> { if (!disposed) { if (reset) resetContent(); applyAppearance(); } });
        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.getEmptyText().setText("选择版块开始浏览");
        threadList.setCellRenderer(new ColoredListCellRenderer<Thread>() {
            @Override protected void customizeCellRenderer(@NotNull JList<? extends Thread> list, Thread value,
                                                           int index, boolean selected, boolean hasFocus) {
                if (value == null) return;
                append(PostText.plain(value.getSubject(), Config.getInstance().getUrl()));
                append("  [" + value.getReplies() + "]  " + value.getAuthor());
            }
        });
        threadList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Thread selected = threadList.getSelectedValue();
            if (selected != null) {
                try { subject = PostText.plain(selected.getSubject(), Config.getInstance().getUrl());
                    getPostByThreadId(Integer.parseInt(selected.getTid())); }
                catch (NumberFormatException ex) { status.setText("无效的主题 ID"); }
            }
        });
        filter.setToolTipText("筛选当前页的标题或作者");
        filter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterThreads(); }
            public void removeUpdate(DocumentEvent e) { filterThreads(); }
            public void changedUpdate(DocumentEvent e) { filterThreads(); }
        });
        postText.setEditable(false); postText.setLineWrap(true); postText.setWrapStyleWord(true);
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(filter, BorderLayout.NORTH); left.add(new JBScrollPane(threadList), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, new JBScrollPane(postText));
        split.setResizeWeight(0.3); split.setDividerLocation(320);

        List<AnAction> actions = new ArrayList<>();
        actions.add(new ThreadFirstPageAction(this)); actions.add(new ThreadPrevPageAction(this));
        actions.add(threadPageAction); actions.add(new ThreadNextPageAction(this));
        actions.add(command("主题页",AllIcons.Actions.ListFiles, () -> jump(false)));
        actions.add(command("主题末页",AllIcons.Actions.Play_last, () -> { if (!threadLoading()) loadThreads(threadMax); }));
        actions.add(new Separator());
        actions.add(new PostFirstPageAction(this)); actions.add(new PostPrevPageAction(this));
        actions.add(postPageAction); actions.add(new PostNextPageAction(this)); actions.add(new PostLastPageAction(this));
        actions.add(command("楼层页", AllIcons.Actions.ListFiles,() -> jump(true)));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Stage1st.Paging",
                new DefaultActionGroup(actions), true);
        toolbar.setTargetComponent(this);

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolbar.getComponent(), BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        button(buttons, "刷新", this::refresh);
        button(buttons, "打开 ID", this::openId);
        button(buttons, "查找", this::find);
        button(buttons, "发帖", () -> compose(false, false));
        button(buttons, "回复", () -> compose(true, false));
        button(buttons, "引用", () -> compose(true, true));
        button(buttons, "书签", this::bookmarks);
        button(buttons, "浏览器", () -> BrowserUtil.browse(Config.getInstance().getUrl()
                + (currentThread > 0 ? "forum.php?mod=viewthread&tid=" + currentThread + "&page=" + postPage
                : currentForum > 0 ? "forum.php?mod=forumdisplay&fid=" + currentForum : "")));
        button(buttons, "账号", () -> new AccountDialog(project).show());
        button(buttons, "隐藏", () -> HideReaderAction.hide(project));
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH); add(split, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        applyAppearance();
        resetPosts();
    }

    private static AnAction command(String name, Icon icon, Runnable run) {
        return new AnAction(name,name, icon) { @Override public void actionPerformed(@NotNull AnActionEvent e) { run.run(); } };
    }
    private static void button(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text); button.addActionListener(e -> action.run()); panel.add(button);
    }

    private void applyAppearance() {
        Font font = UIManager.getFont("TextArea.font");
        if (Config.getInstance().isCamouflage() && font != null) {
            postText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, font.getSize()));
        } else if (font != null) postText.setFont(font);
        com.github.reedoverflow.stage1streader.ui.WindowAppearance.apply(project);
    }

    public void getThreadByForumId(int forumId) {
        if (forumId <= 0 || disposed) return;
        currentForum = forumId; threadPage = 1; threadMax = 1;
        types = Collections.emptyMap(); typeRequired = false;
        loadThreads(1);
    }

    private void loadThreads(int page) {
        if (currentForum <= 0 || disposed) return;
        final int fid = currentForum;
        final long request = ++threadVersion;
        final DiscuzService service = new DiscuzService();
        if (threadWorker != null) threadWorker.cancel(true);
        forumLoaded = false;
        resetPosts();
        threads = Collections.emptyList(); filterThreads();
        threadList.setPaintBusy(true); threadList.getEmptyText().setText("正在加载…");
        threadWorker = new SwingWorker<ForumPage<Thread>, Void>() {
            @Override protected ForumPage<Thread> doInBackground() { return service.getThreadPage(fid, page); }
            @Override protected void done() {
                if (disposed || request != threadVersion || !service.isCurrent()) return;
                try {
                    ForumPage<Thread> result = get();
                    threadPage = result.page; threadMax = result.maxPage;
                    threadPageAction.setPage(threadPage, threadMax);
                    threads = result.items; types = result.threadTypes; typeRequired = result.typeRequired;
                    forumLoaded = true;
                    filterThreads();
                    status.setText("fid=" + fid + " · " + threads.size() + " 个主题");
                } catch (CancellationException ignored) { }
                catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { threadList.getEmptyText().setText("加载失败，请刷新"); error(e); }
                finally { threadList.setPaintBusy(false); }
            }
        };
        threadWorker.execute();
    }

    private void filterThreads() {
        String query = filter.getText().trim().toLowerCase(Locale.ROOT);
        DefaultListModel<Thread> model = new DefaultListModel<>();
        for (Thread thread : threads) {
            if (thread != null && (PostText.plain(thread.getSubject(), Config.getInstance().getUrl())
                    + " " + thread.getAuthor()).toLowerCase(Locale.ROOT).contains(query)) model.addElement(thread);
        }
        threadList.setModel(model);
        threadList.getEmptyText().setText(query.isEmpty() ? "本页没有主题" : "当前页没有匹配结果");
    }

    public void getPostByThreadId(int threadId) {
        if (threadId <= 0 || disposed) return;
        resetPosts(); currentThread = threadId;
        loadPosts(1);
    }

    private void loadPosts(int page) {
        if (currentThread <= 0 || disposed) return;
        final int tid = currentThread;
        final long request = ++postVersion;
        final DiscuzService service = new DiscuzService();
        if (postWorker != null) postWorker.cancel(true);
        posts = Collections.emptyList(); postOffsets.clear();
        postText.setText("正在加载…");
        postWorker = new SwingWorker<ForumPage<Reply>, Void>() {
            @Override protected ForumPage<Reply> doInBackground() { return service.getReplyPage(tid, page); }
            @Override protected void done() {
                if (disposed || request != postVersion || !service.isCurrent()) return;
                try {
                    ForumPage<Reply> result = get();
                    postPage = result.page; postMax = result.maxPage; replyForum = result.forumId;
                    subject = PostText.plain(result.subject, service.root()); posts = result.items;
                    postPageAction.setPage(postPage, postMax);
                    render(service.root());
                    status.setText(subject + " · tid=" + tid);
                } catch (CancellationException ignored) { }
                catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { postText.setText("加载失败，可点击刷新重试。"); error(e); }
            }
        };
        postWorker.execute();
    }

    private void render(String root) {
        postOffsets.clear();
        StringBuilder text = new StringBuilder();
        for (Reply reply : posts) {
            postOffsets.add(text.length());
            text.append("// ").append(reply.getPosition()).append(" · ").append(reply.getAuthor())
                    .append(" · ").append(reply.getDateline()).append('\n');
            text.append(PostText.plain(reply.getMessage(), root)).append('\n');
            if (reply.getAttachment() != null && !"0".equals(reply.getAttachment())) {
                text.append("[本楼含附件，可在浏览器中查看]\n");
            }
            text.append('\n');
        }
        postText.setText(text.length() == 0 ? "本页没有内容。" : text.toString());
        postText.setCaretPosition(0);
    }

    private void compose(boolean reply, boolean quote) {
        if (!SessionManager.getInstance().current().loggedIn()) { new AccountDialog(project).show(); return; }
        if (reply && (currentThread <= 0 || posts.isEmpty() || postLoading())) {
            status.setText("请先打开一个主题，等待内容加载完成。"); return;
        }
        if (!reply && (currentForum <= 0 || !forumLoaded || threadLoading())) {
            status.setText("请先选择版块，等待主题列表加载完成。"); return;
        }
        String draft = "";
        if (quote) {
            int selected = 0;
            for (int i = 0; i < postOffsets.size(); i++) if (postOffsets.get(i) <= postText.getCaretPosition()) selected = i;
            Reply post = posts.get(selected);
            String content = PostText.plain(post.getMessage(), Config.getInstance().getUrl());
            if (content.length() > 600) content = content.substring(0, 600) + "…";
            String author = String.valueOf(post.getAuthor()).replace('[', '(').replace(']', ')');
            draft = "[quote][url=" + Config.getInstance().getUrl()
                    + "forum.php?mod=redirect&goto=findpost&pid=" + post.getPid() + "&ptid=" + currentThread
                    + "]" + author + " 发表于 " + post.getDateline() + "[/url]\n"
                    + content.replace('[', '(').replace(']', ')') + "[/quote]\n\n";
        }
        final int tid = reply ? currentThread : 0;
        new ComposeDialog(project, new DiscuzService(), reply ? replyForum : currentForum,
                tid, reply ? subject : "", draft, types, typeRequired, result -> {
                    Messages.showInfoMessage(project, result.message, "提交结果");
                    if (result.moderated) { status.setText("内容已提交，等待版主审核。"); return; }
                    if (tid > 0 && tid == currentThread) loadPosts(postPage);
                    else if (result.threadId > 0) getPostByThreadId(result.threadId);
                    else if (currentForum > 0) loadThreads(1);
                }).show();
    }

    private void jump(boolean post) {
        if (post ? currentThread == 0 || postLoading() : currentForum == 0 || threadLoading()) return;
        int max = post ? postMax : threadMax;
        String value = Messages.showInputDialog(project, "页码（1–" + max + "）", "跳转", null);
        if (value == null) return;
        try {
            int page = Integer.parseInt(value.trim());
            if (page < 1 || page > max) throw new NumberFormatException();
            if (post) loadPosts(page); else loadThreads(page);
        } catch (NumberFormatException e) { status.setText("请输入有效页码。"); }
    }

    private void openId() {
        String value = Messages.showInputDialog(project, "输入主题 tid", "打开主题", null);
        if (value == null) return;
        try {
            int tid = Integer.parseInt(value.trim());
            if (tid <= 0) throw new NumberFormatException();
            getPostByThreadId(tid);
        } catch (NumberFormatException e) { status.setText("请输入正整数主题 ID。"); }
    }

    private void find() {
        String value = Messages.showInputDialog(project, "查找当前页正文", "查找", null);
        if (value == null || value.isEmpty()) return;
        String text = postText.getText();
        int start = text.indexOf(value, postText.getSelectionEnd());
        if (start < 0) start = text.indexOf(value);
        if (start < 0) status.setText("当前页未找到文本。");
        else { postText.requestFocusInWindow(); postText.select(start, start + value.length()); }
    }

    private void bookmarks() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem save = new JMenuItem("保存当前阅读位置");
        save.setEnabled(currentThread > 0 && !posts.isEmpty());
        save.addActionListener(e -> Config.getInstance().saveBookmark(currentThread, postPage, subject));
        menu.add(save);
        for (Config.Bookmark bookmark : Config.getInstance().getBookmarks()) {
            JMenu entry = new JMenu(bookmark.title + " · " + bookmark.page);
            JMenuItem open = new JMenuItem("打开");
            open.addActionListener(e -> {
                resetPosts(); currentThread = bookmark.threadId; loadPosts(bookmark.page);
            });
            JMenuItem remove = new JMenuItem("删除");
            remove.addActionListener(e -> Config.getInstance().removeBookmark(bookmark.threadId));
            entry.add(open); entry.add(remove); menu.add(entry);
        }
        menu.show(this, 20, 60);
    }

    private void refresh() {
        if (currentThread > 0) loadPosts(postPage);
        else if (currentForum > 0) loadThreads(threadPage);
    }
    private void resetPosts() {
        ++postVersion; if (postWorker != null) postWorker.cancel(true);
        currentThread = 0; replyForum = 0; postPage = 1; postMax = 1;
        posts = Collections.emptyList(); postOffsets.clear();
        postPageAction.setPage(1, 1); postText.setText("选择主题开始阅读。");
    }
    private void resetContent() {
        ++threadVersion; if (threadWorker != null) threadWorker.cancel(true);
        currentForum = 0; threadPage = 1; threadMax = 1;
        forumLoaded = false;
        types = Collections.emptyMap(); typeRequired = false;
        threads = Collections.emptyList(); filterThreads(); resetPosts();
        threadPageAction.setPage(1, 1); threadList.setPaintBusy(false);
        status.setText("站点或账号已更新，请重新选择版块。");
    }
    private void error(ExecutionException error) {
        Throwable cause = error.getCause();
        status.setText(cause == null ? "加载失败" : cause.getMessage());
    }
    private boolean threadLoading() { return threadWorker != null && !threadWorker.isDone(); }
    private boolean postLoading() { return postWorker != null && !postWorker.isDone(); }

    public void getFirstPageThread() { if (!threadLoading()) loadThreads(1); }
    public void getPrevPageThread() { if (!threadLoading() && threadPage > 1) loadThreads(threadPage - 1); }
    public void getNextPageThread() { if (!threadLoading() && threadPage < threadMax) loadThreads(threadPage + 1); }
    public void getFirstPagePost() { if (!postLoading()) loadPosts(1); }
    public void getPrevPagePost() { if (!postLoading() && postPage > 1) loadPosts(postPage - 1); }
    public void getNextPagePost() { if (!postLoading() && postPage < postMax) loadPosts(postPage + 1); }
    public void getLastPagePost() { if (!postLoading()) loadPosts(postMax); }

    @Override public void dispose() {
        disposed = true; ++threadVersion; ++postVersion;
        if (threadWorker != null) threadWorker.cancel(true);
        if (postWorker != null) postWorker.cancel(true);
    }
}
