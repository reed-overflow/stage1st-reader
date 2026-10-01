package com.github.reedoverflow.stage1streader.settings;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.github.reedoverflow.stage1streader.service.SessionManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

/** Application-wide settings, including automatically persisted login cookies. */
public class Stage1stReaderConfigurable implements Configurable {
    private JPanel panel;
    private JTextField urlField;
    private JCheckBox camouflage;

    @Override @Nls public String getDisplayName() { return "Stage1st Reader"; }
    @Override @Nullable public JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel(new BorderLayout(8, 8));
            JPanel fields = new JPanel(new GridLayout(0, 1, 8, 8));
            fields.add(new JLabel("论坛根地址（包括 /2b/，不包括 forum.php 或查询参数）"));
            urlField = new JTextField();
            urlField.setToolTipText(Config.DEFAULT_URL);
            fields.add(urlField);
            camouflage = new JCheckBox("伪装模式：Index / Output 窗口名、等宽纯文本阅读");
            fields.add(camouflage);
            JTextArea help = new JTextArea("默认地址：" + Config.DEFAULT_URL
                    + "\n更改站点会清除当前登录和阅读内容，所有项目共用此设置。"
                    + "\n登录 / Cookie 导入和退出登录位于版块窗口工具栏。"
                    + "\n登录 Cookie 和书签自动保存在本机设置中，重启 IDE 后恢复登录。"
                    + "\n退出登录或更改站点会清除保存的 Cookie；密码和安全提问答案不保存。"
                    + "\nCtrl+Alt+Shift+F12 隐藏两个阅读窗口，可在 Keymap 中修改。");
            help.setEditable(false); help.setOpaque(false);
            fields.add(help); panel.add(fields, BorderLayout.NORTH);
        }
        reset(); return panel;
    }
    @Override public boolean isModified() {
        return urlField != null && (!Config.normalizeUrl(urlField.getText()).equals(Config.getInstance().getUrl())
                || camouflage.isSelected() != Config.getInstance().isCamouflage());
    }
    @Override public void apply() throws ConfigurationException {
        if (urlField == null) return;
        String value = Config.normalizeUrl(urlField.getText());
        if (!Config.isValidUrl(value)) throw new ConfigurationException("请输入有效的 HTTP(S) 论坛根地址，不包含账号、查询参数或片段。");
        Config config = Config.getInstance();
        config.setUrl(value);
        // Close the previous jar even when no reader window is open.
        SessionManager.getInstance().current();
        config.setCamouflage(camouflage.isSelected());
        urlField.setText(value);
    }
    @Override public void reset() {
        if (urlField != null) {
            urlField.setText(Config.getInstance().getUrl());
            camouflage.setSelected(Config.getInstance().isCamouflage());
        }
    }
    @Override public void disposeUIResources() { panel = null; urlField = null; camouflage = null; }
}
