package cnnr.groundwork.client;

/** Client-side mirror of the server's per-player build-vision state, set from {@code BuildVisionPayload}. */
public final class ClientBuildVision {
    private static boolean active = false;

    private ClientBuildVision() {}

    public static boolean isActive() { return active; }
    public static void set(boolean v) { active = v; }
}
