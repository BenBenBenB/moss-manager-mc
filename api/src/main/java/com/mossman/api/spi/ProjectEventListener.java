package com.mossman.api.spi;

import com.mossman.api.event.ProjectEvent;

@FunctionalInterface
public interface ProjectEventListener {

    void onProjectEvent(ProjectEvent event);
}
