package com.github.reedoverflow.stage1streader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();

        JPanel mainPanel = new JPanel();
        Button button = new Button();
        button.setLabel("click me");
        mainPanel.add(button);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 第一个参数指定父组件，不为空时，在父组件的中心弹出，为空时，在屏幕的中心弹出
                JOptionPane.showMessageDialog(null, "Hello, this is my first program.", "Hello world", JOptionPane.INFORMATION_MESSAGE);
                if (JOptionPane.showConfirmDialog(jFrame, "确定关闭吗？", "确认", JOptionPane.OK_CANCEL_OPTION) == 0) {
                    System.exit(0);
                }
            }
        });

        jFrame.add(mainPanel);
        jFrame.setSize(400, 400);
        jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);不设置时，点击取消时，窗口关闭，但程序不会shutdown
                if (JOptionPane.showConfirmDialog(jFrame, "确定关闭吗？", "确认", JOptionPane.OK_CANCEL_OPTION) == 0) {
                    System.exit(0);
                }
            }
        });
        jFrame.setVisible(true);
    }
}
