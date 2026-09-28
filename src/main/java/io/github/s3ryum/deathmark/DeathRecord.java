package io.github.s3ryum.deathmark;

import java.util.UUID;

record DeathRecord(UUID worldId, String worldName, double x, double y, double z, long recordedAt) {
}
