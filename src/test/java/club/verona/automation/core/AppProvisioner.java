package club.verona.automation.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Resolves the Verona APK used for remote-grid sessions. The build lives in
 * apps/ at the project root — if it's already there it's used as-is; if
 * missing and app.download.url is configured, it's fetched once and cached
 * there for every later run.
 */
final class AppProvisioner {

    private static final Path APPS_DIR = Path.of("apps");

    private AppProvisioner() {}

    static String resolve(String fileName, String downloadUrl) {
        Path apk = APPS_DIR.resolve(fileName);
        if (Files.isRegularFile(apk)) {
            return apk.toAbsolutePath().toString();
        }
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalStateException(
                    "Verona APK not found at " + apk.toAbsolutePath()
                            + ". Place the build there, or set app.download.url in verona.properties "
                            + "to fetch it automatically.");
        }
        download(downloadUrl, apk);
        return apk.toAbsolutePath().toString();
    }

    private static void download(String url, Path dest) {
        try {
            Files.createDirectories(dest.getParent());
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to download Verona APK from " + url, e);
        }
    }
}
