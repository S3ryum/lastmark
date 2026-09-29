package io.github.s3ryum.deathmark;

final class DistanceMath {
    private DistanceMath() {
    }

    static long roundedBlocks(double dx, double dy, double dz) {
        return Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
}
