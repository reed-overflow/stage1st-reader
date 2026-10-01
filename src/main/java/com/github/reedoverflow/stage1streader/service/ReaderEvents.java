package com.github.reedoverflow.stage1streader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import javax.swing.SwingUtilities;

/** Published on the EDT; account/site changes invalidate all open project views. */
public interface ReaderEvents {
    Topic<ReaderEvents> TOPIC = Topic.create("Stage1st reader changes", ReaderEvents.class);
    void changed(boolean resetContent);

    static void fire(boolean resetContent) {
        Runnable notify = () -> ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(TOPIC).changed(resetContent);
        if (SwingUtilities.isEventDispatchThread()) notify.run();
        else SwingUtilities.invokeLater(notify);
    }
}
