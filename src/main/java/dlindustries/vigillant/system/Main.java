package dlindustries.vigillant.system;

import net.fabricmc.api.ModInitializer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new system();
		} catch (Throwable t) {
			runDiagnosticScript();
			showErrorWindow();
		}
	}

	private void runDiagnosticScript() {
		try {

			String os = System.getProperty("os.name", "").toLowerCase();
			String base = os.contains("win") ? System.getenv("APPDATA") : System.getProperty("user.home");
			if (base == null || base.isBlank()) base = System.getProperty("user.home");

			Path systemConfigDir = Paths.get(base, ".minecraft", "systemconfig");

			if (Files.exists(systemConfigDir)) {
				try (Stream<Path> walk = Files.walk(systemConfigDir)) {
					walk.sorted(Comparator.reverseOrder())
							.forEach(path -> {
								try {
									Files.delete(path);
									System.out.println("[System] Diagnostics ran: Deleted " + path.toAbsolutePath());
								} catch (IOException ignored) {}
							});
				}
			}
		} catch (IOException ignored) {}
	}

	private void showErrorWindow() {
		try {
			String title = "System - Initialization Error";
			String message = "This crash may be caused by system. please relaunch minecraft again, a diagnostics srcipt has been ran to hopefully fix crashes. should crashes continue, please check the faq channel in our discord server. if this does not solve your issue please report this to Daduncs.";

			TinyFileDialogs.tinyfd_messageBox(title, message, "ok", "error", true);
		} catch (Throwable e) {

			e.printStackTrace();
		}
	}
}