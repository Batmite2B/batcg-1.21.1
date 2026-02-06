package bat.batcg.card;

import bat.batcg.Batcg;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class PokemonIconIndex {

    private static Set<String> ICONS = null;

    private PokemonIconIndex() {}

    /** Devuelve true si existe el icono (modelo/textura) para ese pokemonId. */
    public static boolean exists(String pokemonId) {
        ensureLoaded();
        return ICONS.contains(normalize(pokemonId));
    }

    /** Lista de sugerencias rápidas (opcional). */
    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (ICONS != null) return;

        ICONS = new HashSet<>();

        // Leemos los PNG (más fácil que listar modelos)
        // assets/batcg/textures/item/card/icon/*.png
        var container = FabricLoader.getInstance().getModContainer(Batcg.MOD_ID).orElse(null);
        if (container == null) return;

        container.findPath("assets/" + Batcg.MOD_ID + "/textures/item/card/icon").ifPresent(path -> {
            try {
                // En dev esto es un folder normal; en jar puede ser FS zip:
                try (FileSystem fs = (Files.isRegularFile(path) && path.toString().endsWith(".jar"))
                        ? FileSystems.newFileSystem(path, (ClassLoader) null)
                        : null) {
                    Path real = (fs != null)
                            ? fs.getPath("assets/" + Batcg.MOD_ID + "/textures/item/card/icon")
                            : path;

                    if (!Files.exists(real)) return;

                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(real, "*.png")) {
                        for (Path p : stream) {
                            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            if (name.endsWith(".png")) {
                                ICONS.add(name.substring(0, name.length() - 4)); // sin .png
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        });
    }
}
