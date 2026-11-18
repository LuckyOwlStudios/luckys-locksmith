package net.luckyowlstudios.locksmith.platform;

import net.luckyowlstudios.locksmith.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Loads the loader-specific implementations of the service interfaces in {@code platform.services}.
 * Each loader module lists its implementation in {@code META-INF/services}.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
