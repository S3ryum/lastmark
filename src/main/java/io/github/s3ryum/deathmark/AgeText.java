package io.github.s3ryum.deathmark;

final class AgeText {
    private static final long MINUTE_MILLIS = 60_000L;

    private AgeText() {
    }

    static String format(long recordedAtMillis, long nowMillis) {
        long elapsedMillis = nowMillis <= recordedAtMillis ? 0 : nowMillis - recordedAtMillis;
        long totalMinutes = elapsedMillis / MINUTE_MILLIS;
        if (totalMinutes == 0) {
            return "less than a minute ago";
        }

        long[] amounts = {
                totalMinutes / (24 * 60),
                (totalMinutes % (24 * 60)) / 60,
                totalMinutes % 60,
        };
        String[] units = {"day", "hour", "minute"};
        StringBuilder result = new StringBuilder();
        int includedUnits = 0;
        for (int index = 0; index < amounts.length && includedUnits < 2; index++) {
            long amount = amounts[index];
            if (amount == 0) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(amount).append(' ').append(units[index]);
            if (amount != 1) {
                result.append('s');
            }
            includedUnits++;
        }
        return result.append(" ago").toString();
    }
}
