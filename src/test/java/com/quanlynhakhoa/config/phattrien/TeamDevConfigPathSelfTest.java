package com.quanlynhakhoa.config.phattrien;

import java.nio.file.*;
import java.util.Comparator;

/** Synthetic Git metadata/config paths only; no credentials, JDBC or external Git process. */
public final class TeamDevConfigPathSelfTest {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
    public static void main(String[] args) throws Exception {
        Path fixture = Files.createTempDirectory("n08-worktree-config-").toAbsolutePath().normalize();
        try {
            Path main = fixture.resolve("main"), checkout = fixture.resolve("checkout");
            Path common = main.resolve(".git"), metadata = common.resolve("worktrees/check-out");
            Path shared = main.resolve(".n08-dev.local.properties");
            Path local = checkout.resolve(".n08-dev.local.properties");
            Files.createDirectories(checkout);
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Ordinary checkout keeps local behavior");
            write(shared, "# Synthetic path fixture; no credentials\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Never search arbitrary sibling or ancestor configs");
            write(checkout.resolve(".git"), "gitdir: " + metadata + "\n");
            write(metadata.resolve("commondir"), "../..\n");
            write(metadata.resolve("gitdir"), checkout.resolve(".git") + "\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(shared), "Linked worktree uses main checkout config");
            String relative = checkout.relativize(metadata).toString();
            write(checkout.resolve(".git"), "gitdir: " + relative + "\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(shared), "Relative Git metadata path supported");
            write(local, "# Local config has priority even when its contents are invalid\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Local config must not silently fall back on validation failure");
            Files.delete(local);
            write(metadata.resolve("gitdir"), fixture.resolve("different/.git") + "\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Wrong checkout backlink rejected");
            write(metadata.resolve("gitdir"), checkout.resolve(".git") + "\n");
            write(metadata.resolve("commondir"), "\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Empty common directory rejected");
            write(metadata.resolve("commondir"), "../..\n");
            Files.delete(shared);
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Missing shared config keeps synthetic fallback");
            write(shared, "# Synthetic only\n");
            write(checkout.resolve(".git"), "not Git metadata\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Malformed Git pointer rejected");
            write(checkout.resolve(".git"), "gitdir: " + fixture.resolve("missing") + "\n");
            check(TeamDevConnection.resolveConfigPath(checkout).equals(local), "Missing metadata does not crash startup");
            check(TeamDevConnection.resolveConfigPath(main).equals(shared), "Main checkout continues using its own config");
            System.out.println("PASS TeamDevConfigPathSelfTest: " + checks + " worktree/config path checks; no database access.");
        } finally {
            try (var paths = Files.walk(fixture)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.toAbsolutePath().normalize().startsWith(fixture))
                        throw new AssertionError("Cleanup escaped the synthetic fixture directory");
                    Files.delete(path);
                }
            }
        }
    }
}
