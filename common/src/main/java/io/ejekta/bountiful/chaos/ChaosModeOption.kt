package io.ejekta.bountiful.chaos

enum class ChaosModeOption {
    /** Chaos Mode is disabled. Normal decrees and pools are used exclusively. */
    OFF,

    /** Chaos Mode is enabled exclusively. All normal decrees and pools are replaced by the generated chaos decree. */
    ON,

    /** Chaos Mode runs alongside normal decrees. The chaos decree is added as a peer and can spawn like any other decree. */
    COMBINED
}
