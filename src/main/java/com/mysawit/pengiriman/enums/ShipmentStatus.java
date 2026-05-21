package com.mysawit.pengiriman.enums;

import java.util.Set;

/**
 * State Pattern: each status declares its own valid transitions and actor.
 */
public enum ShipmentStatus {

    MEMUAT {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of(MENGIRIM);
        }

        @Override
        public String actor() {
            return "DRIVER";
        }
    },

    MENGIRIM {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of(TIBA_DI_TUJUAN);
        }

        @Override
        public String actor() {
            return "DRIVER";
        }
    },

    TIBA_DI_TUJUAN {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of(DISETUJUI_MANDOR, DITOLAK_MANDOR);
        }

        @Override
        public String actor() {
            return "MANDOR";
        }
    },

    DISETUJUI_MANDOR {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of(DISETUJUI_ADMIN, DITOLAK_ADMIN, DITOLAK_PARSIAL_ADMIN);
        }

        @Override
        public String actor() {
            return "ADMIN";
        }
    },

    DITOLAK_MANDOR {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of();
        }

        @Override
        public String actor() {
            return "NONE";
        }
    },

    DISETUJUI_ADMIN {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of();
        }

        @Override
        public String actor() {
            return "NONE";
        }
    },

    DITOLAK_ADMIN {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of();
        }

        @Override
        public String actor() {
            return "NONE";
        }
    },

    DITOLAK_PARSIAL_ADMIN {
        @Override
        public Set<ShipmentStatus> allowedTransitions() {
            return Set.of();
        }

        @Override
        public String actor() {
            return "NONE";
        }
    };

    public abstract Set<ShipmentStatus> allowedTransitions();

    public abstract String actor();

    public boolean canTransitionTo(ShipmentStatus next) {
        return allowedTransitions().contains(next);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    public boolean isDriverMutable() {
        return "DRIVER".equals(actor());
    }
}
