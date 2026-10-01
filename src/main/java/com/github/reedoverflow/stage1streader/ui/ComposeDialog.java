package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** The editor is the review step. Only pressing Publish performs a single POST. */
public final class ComposeDialog extends DialogWrapper {
    private final DiscuzService service;
    private final Project project;
    private final int forumId;
    private final int threadId;
    private final boolean typeRequired;
    private final Map<String, String> types;
    private final Consumer<DiscuzService.Submission> submitted;
    private final JTextField subject = new JTextField();
    private final JBTextArea message = new JBTextArea(15, 65);
    private final JComboBox<String> type = new JComboBox<>();
    private final JTextArea status = new JTextArea(3, 65);
    private boolean busy;

    public ComposeDialog(Project project, DiscuzService service, int forumId, int threadId,
                         String title, String draft, Map<String, String> types, boolean required,
                         Consumer<DiscuzService.Submission> submitted) {
        super(project);
        this.project = project; this.service = service; this.forumId = forumId; this.threadId = threadId;
        this.types = types; this.typeRequired = required; this.submitted = submitted;
        subject.setText(threadId > 0 ? "" : title);
        message.setText(draft);
        setTitle(threadId > 0 ? "回复：" + title : "发表主题 · fid=" + forumId);
        setOKButtonText("发布到论坛");
        init();
    }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel header = new JPanel(new GridLayout(0, 1, 4, 4));
        header.add(new JLabel(service.root() + (threadId > 0 ? " · tid=" + threadId : " · fid=" + forumId)));
        if (threadId == 0) {
            header.add(new JLabel("标题")); header.add(subject);
            type.addItem("选择主题分类" + (typeRequired ? "（必选）" : "（可选）"));
            types.forEach((id, label) -> type.addItem(label));
            header.add(type);
        }
        header.add(new JLabel("正文支持 Discuz BBCode；发布前请核对站点和主题。"));
        panel.add(header, BorderLayout.NORTH);
        message.setLineWrap(true); message.setWrapStyleWord(true);
        panel.add(new JBScrollPane(message), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true);
        footer.add(new JBScrollPane(status), BorderLayout.CENTER);
        JButton browser = new JButton("在浏览器打开编辑页");
        browser.addActionListener(e -> BrowserUtil.browse(service.root() + "forum.php?mod=post&action="
                + (threadId > 0 ? "reply&tid=" + threadId : "newthread&fid=" + forumId)));
        footer.add(browser, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    @Override protected void doOKAction() {
        if (busy) return;
        if (!service.isCurrent()) { status.setText("站点或账号已切换，请复制草稿后重新打开编辑器。"); return; }
        String title = subject.getText().trim();
        String body = message.getText();
        if (body.trim().isEmpty() || threadId == 0 && title.isEmpty()) {
            status.setText("标题和正文不能为空。"); return;
        }
        if (threadId == 0 && typeRequired && type.getSelectedIndex() == 0) {
            status.setText("当前版块要求选择主题分类。"); return;
        }
        final String typeId = threadId == 0 && type.getSelectedIndex() > 0
                ? types.keySet().toArray(new String[0])[type.getSelectedIndex() - 1] : "0";
        busy = true; setOKActionEnabled(false); setCancelButtonText("发送中");
        message.setEditable(false); subject.setEditable(false); type.setEnabled(false);
        status.setText("正在提交，请稍候…");
        new SwingWorker<DiscuzService.Submission, Void>() {
            @Override protected DiscuzService.Submission doInBackground() {
                return service.submit(forumId, threadId, title, body, typeId);
            }
            @Override protected void done() {
                busy = false; setOKActionEnabled(true); setCancelButtonText("取消");
                message.setEditable(true); subject.setEditable(true); type.setEnabled(true);
                if (project.isDisposed()) { close(CANCEL_EXIT_CODE); return; }
                try {
                    DiscuzService.Submission result = get();
                    close(OK_EXIT_CODE);
                    if (service.isCurrent()) submitted.accept(result);
                } catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { status.setText(e.getCause().getMessage()); }
            }
        }.execute();
    }

    @Override public void doCancelAction() { if (!busy) super.doCancelAction(); }
}
