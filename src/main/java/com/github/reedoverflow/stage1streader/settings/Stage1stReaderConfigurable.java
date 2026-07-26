package com.github.reedoverflow.stage1streader.settings;

import com.github.reedoverflow.stage1streader.constant.Config;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings page shown under Settings | Tools | Stage1st Reader.
 */
public class Stage1stReaderConfigurable implements Configurable {

    private JPanel panel;
    private JTextField urlField;

    @Override
    @Nls
    public String getDisplayName() {
        return "Stage1st Reader";
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());

            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = 0;
            labelConstraints.anchor = GridBagConstraints.WEST;
            labelConstraints.insets = new Insets(0, 0, 0, 8);
            panel.add(new JLabel("Forum URL:"), labelConstraints);

            urlField = new JTextField();
            urlField.setToolTipText("The forum root URL, for example https://www.saraba1st.com/2b/");
            GridBagConstraints fieldConstraints = new GridBagConstraints();
            fieldConstraints.gridx = 1;
            fieldConstraints.gridy = 0;
            fieldConstraints.weightx = 1.0;
            fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(urlField, fieldConstraints);

            GridBagConstraints fillerConstraints = new GridBagConstraints();
            fillerConstraints.gridx = 0;
            fillerConstraints.gridy = 1;
            fillerConstraints.gridwidth = 2;
            fillerConstraints.weighty = 1.0;
            fillerConstraints.fill = GridBagConstraints.VERTICAL;
            panel.add(Box.createVerticalGlue(), fillerConstraints);
        }

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        if (urlField == null) {
            return false;
        }
        String configuredUrl = Config.getInstance().getUrl();
        return !Config.normalizeUrl(urlField.getText()).equals(configuredUrl);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (urlField == null) {
            return;
        }

        String value = Config.normalizeUrl(urlField.getText());
        if (!Config.isValidUrl(value)) {
            throw new ConfigurationException("Please enter a valid HTTP or HTTPS forum URL.");
        }
        Config.getInstance().setUrl(value);
        urlField.setText(value);
    }

    @Override
    public void reset() {
        if (urlField != null) {
            urlField.setText(Config.getInstance().getUrl());
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        urlField = null;
    }
}
