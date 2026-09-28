package dev.ajvanegasv.kontio.domain.model

/**
 * Representa la preferencia de tema visual de la aplicación Kontio.
 */
enum class ThemeMode {
    /**
     * Sigue la apariencia configurada en el sistema operativo del dispositivo móvil (por defecto).
     */
    SYSTEM,

    /**
     * Fuerza la apariencia en modo claro.
     */
    LIGHT,

    /**
     * Fuerza la apariencia en modo oscuro.
     */
    DARK
}
