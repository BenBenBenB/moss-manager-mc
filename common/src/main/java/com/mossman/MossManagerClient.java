package com.mossman;

import com.mossman.network.ClientProjects;

/** Client-only entry. Called from each loader's client init class. */
public final class MossManagerClient {

    private MossManagerClient() {}

    public static void init() {
        ClientProjects.register();
    }
}
