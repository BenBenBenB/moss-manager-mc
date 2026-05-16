package com.mossman;

import com.mossman.network.Packets;
import com.mossman.network.ServerProjectSync;
import com.mossman.server.ServerProjects;

public final class MossManager {
    public static final String MOD_ID = "moss_manager_mc";

    public static void init() {
        ServerProjects.register();
        Packets.register();
        ServerProjectSync.register();
    }
}
