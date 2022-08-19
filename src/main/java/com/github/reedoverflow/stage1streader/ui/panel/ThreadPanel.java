package com.github.reedoverflow.stage1streader.ui.panel;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

public class ThreadPanel extends JPanel {

    private JBTextArea textArea = new JBTextArea();

    public ThreadPanel() {
        super();
        textArea.setText("123");
        this.add(textArea);
    }

    public void setText(String text) {
        textArea.setText(text);
    }
}
