package com.github.reedoverflow.stage1streader.ui.panel;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

public class ThreadPanel extends JPanel {

    public ThreadPanel() {
        super();


        JBTextArea textArea = new JBTextArea();
        textArea.setText("123");

        this.add(textArea);
    }
}
