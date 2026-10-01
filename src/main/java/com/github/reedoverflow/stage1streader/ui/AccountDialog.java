package com.github.reedoverflow.stage1streader.ui;

import com.github.reedoverflow.stage1streader.service.DiscuzService;
import com.github.reedoverflow.stage1streader.service.SessionManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/** Login is explicit. Browser challenges can be completed outside the IDE. */
public final class AccountDialog extends DialogWrapper {
    private final DiscuzService service = new DiscuzService();
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField cookies = new JPasswordField();
    private final JComboBox<String> loginField = new JComboBox<>(new String[]{"username", "uid", "email", "mobile"});
    private final JComboBox<String> question = new JComboBox<>(new String[]{
            "未设置安全提问", "母亲的名字", "爷爷的名字", "父亲出生的城市", "其中一位老师的名字",
            "个人计算机的型号", "最喜欢的餐馆名称", "驾驶执照最后四位数字"});
    private final JPasswordField answer = new JPasswordField();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextArea status = new JTextArea(3, 46);
    private boolean busy;
    private final Project project;

    public AccountDialog(Project project) {
        super(project);
        this.project = project;
        setTitle("Stage1st · 登录");
        setOKButtonText("登录 / 导入");
        init();
    }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("站点：" + service.root()), BorderLayout.NORTH);
        JPanel credentials = new JPanel(new GridLayout(0, 2, 8, 8));
        credentials.add(new JLabel("登录方式")); credentials.add(loginField);
        credentials.add(new JLabel("账号")); credentials.add(username);
        credentials.add(new JLabel("密码")); credentials.add(password);
        credentials.add(new JLabel("安全提问")); credentials.add(question);
        credentials.add(new JLabel("答案")); credentials.add(answer);
        tabs.addTab("账号密码", credentials);
        JPanel cookiePanel = new JPanel(new BorderLayout(4, 8));
        JTextArea help = new JTextArea("在浏览器登录此站点后，打开开发者工具 → Network，\n选择该论坛的请求，复制 Request Headers 中完整的 Cookie 值。\n粘贴到下面；不要使用 document.cookie，它会遗漏 HttpOnly 登录凭据。");
        help.setEditable(false); help.setOpaque(false);
        cookiePanel.add(help, BorderLayout.NORTH);
        cookiePanel.add(cookies, BorderLayout.CENTER);
        JButton browser = new JButton("打开浏览器登录");
        browser.addActionListener(e -> BrowserUtil.browse(service.root() + "member.php?mod=logging&action=login"));
        cookiePanel.add(browser, BorderLayout.SOUTH);
        tabs.addTab("浏览器 Cookie", cookiePanel);
        panel.add(tabs, BorderLayout.CENTER);
        status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true);
        SessionManager.Session current = SessionManager.getInstance().current();
        status.setText((current.loggedIn() ? "当前账号：" + current.username + "（UID " + current.uid + "）" : "当前为游客")
                + "\n登录 Cookie 自动保存在本机设置中，重启 IDE 后恢复；密码和答案不保存。"
                + "\n退出登录或更改站点会清除保存的 Cookie；过期后需重新登录。");
        panel.add(new JBScrollPane(status), BorderLayout.SOUTH);
        return panel;
    }

    @Override protected void doOKAction() {
        if (busy) return;
        if (!service.isCurrent()) { status.setText("站点或账号已改变，请关闭此窗口后重新打开。"); return; }
        final boolean importing = tabs.getSelectedIndex() == 1;
        final String user = username.getText().trim();
        final char[] secret = importing ? cookies.getPassword() : password.getPassword();
        final char[] securityAnswer = answer.getPassword();
        if (secret.length == 0 || !importing && user.isEmpty()) {
            status.setText("请填写账号密码或 Cookie。");
            Arrays.fill(secret, '\0'); Arrays.fill(securityAnswer, '\0'); return;
        }
        final String field = (String) loginField.getSelectedItem();
        final int questionId = question.getSelectedIndex();
        busy = true; setOKActionEnabled(false); setCancelButtonText("请稍候");
        status.setText("正在验证登录会话…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try {
                    if (importing) service.importCookies(new String(secret));
                    else service.login(user, new String(secret), field, questionId, new String(securityAnswer));
                    return null;
                } finally { Arrays.fill(secret, '\0'); Arrays.fill(securityAnswer, '\0'); }
            }
            @Override protected void done() {
                busy = false; setOKActionEnabled(true); setCancelButtonText("取消");
                password.setText(""); cookies.setText(""); answer.setText("");
                if (project.isDisposed()) { close(CANCEL_EXIT_CODE); return; }
                try { get(); close(OK_EXIT_CODE); }
                catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
                catch (ExecutionException e) {
                    status.setText(e.getCause().getMessage() + "\n如需验证码或二次验证，请切换到浏览器 Cookie 登录。");
                }
            }
        }.execute();
    }

    @Override public void doCancelAction() { if (!busy) super.doCancelAction(); }
}
