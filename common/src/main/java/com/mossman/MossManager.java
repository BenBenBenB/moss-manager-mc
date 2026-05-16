package com.mossman;

import com.mossman.server.ServerProjects;

public final class MossManager {
    public static final String MOD_ID = "moss_manager_mc";

    public static void init() {
        ServerProjects.register();
    }
}
