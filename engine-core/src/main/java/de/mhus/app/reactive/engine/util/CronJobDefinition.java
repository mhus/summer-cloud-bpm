package de.mhus.app.reactive.engine.util;

import java.util.Calendar;

import org.summerclouds.common.core.tool.MCast;

public class CronJobDefinition {

    public static final long CALCULATE_NEXT = 0;
    public static final long DISABLED_TIME = -1;
    public static final long REMOVE_TIME = -2;

    private int[] allowedMinutes;
    private int[] allowedHours;
    private int[] allowedDaysMonth;
    private int[] allowedMonthes;
    private int[] allowedDaysWeek;
    private String definition;
    private boolean disabled = false;

    public CronJobDefinition() {}

    public CronJobDefinition(String definition) {
        this.definition = definition.trim();
        String[] parts = this.definition.split(" ");
        if (parts.length == 0) {
            parts = new String[] {"disabled"};
        }
        if (parts.length == 1) {
            if (parts[0].equals("disabled"))
                parts = new String[] {"*", "*", "*", "*", "*", "disabled"};
            else {
                int i = MCast.toint(parts[0], 0);
                if (i > 0) {
                    int m = 60 / i % 60;
                    int h = 24 / (i / 60) % 24;
                    parts =
                            new String[] {
                                m > 0 ? "*/" + m : "*", h > 0 ? "*/" + h : "*", "*", "*", "*"
                            };
                }
            }
        }

        if (parts.length > 0) allowedMinutes = MCast.toIntIntervalValues(parts[0], 0, 59);

        if (parts.length > 1) allowedHours = MCast.toIntIntervalValues(parts[1], 0, 23);

        if (parts.length > 2) allowedDaysMonth = MCast.toIntIntervalValues(parts[2], 1, 31);

        if (parts.length > 3) allowedMonthes = MCast.toIntIntervalValues(parts[3], 0, 11);

        if (parts.length > 4) {
            parts[4] = parts[4].toLowerCase();
            parts[4] = parts[4].replace("so", "1");
            parts[4] = parts[4].replace("mo", "2");
            parts[4] = parts[4].replace("tu", "3");
            parts[4] = parts[4].replace("we", "4");
            parts[4] = parts[4].replace("th", "5");
            parts[4] = parts[4].replace("fr", "6");
            parts[4] = parts[4].replace("sa", "7");
            allowedDaysWeek = MCast.toIntIntervalValues(parts[4], 1, 7);
        }

        if (parts.length > 5) {
            if (parts[5].indexOf("disabled") >= 0) disabled = true;
        }
    }

    public long calculateNext(long start) {

        if (disabled) return DISABLED_TIME;

        Calendar next = Calendar.getInstance();
        if (start > 0) next.setTimeInMillis(start);

        // obligatory next minute
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.SECOND, 0);
        next.add(Calendar.MINUTE, 1);

        boolean retry = true;
        int retryCnt = 0;
        while (retry && retryCnt < 10) {
            retry = false;
            retryCnt++;

            if (allowedMinutes != null) {
                int[] d = findNextAllowed(allowedMinutes, next.get(Calendar.MINUTE));
                next.set(Calendar.MINUTE, d[1]);
                if (d[2] == 1) next.add(Calendar.HOUR_OF_DAY, 1);
            }
            if (allowedHours != null) {
                int[] d = findNextAllowed(allowedHours, next.get(Calendar.HOUR_OF_DAY));
                next.set(Calendar.HOUR_OF_DAY, d[1]);
                if (d[2] == 1) next.add(Calendar.DATE, 1);
            }
            if (allowedDaysMonth != null) {
                int[] d = findNextAllowed(allowedDaysMonth, next.get(Calendar.DAY_OF_MONTH));
                next.set(Calendar.DAY_OF_MONTH, d[1]);
                if (d[2] == 1) next.add(Calendar.MONTH, 1);
            }
            if (allowedMonthes != null) {
                int[] d = findNextAllowed(allowedMonthes, next.get(Calendar.MONTH));
                next.set(Calendar.MONTH, d[1]);
                if (d[2] == 1) next.add(Calendar.YEAR, 1);
            }
            if (allowedDaysWeek != null) {
                int[] d = findNextAllowed(allowedDaysWeek, next.get(Calendar.DAY_OF_WEEK));
                if (next.get(Calendar.DAY_OF_WEEK) != d[1]) {
                    next.set(Calendar.DAY_OF_WEEK, d[1]);
                    next.set(Calendar.HOUR, 0);
                    next.set(Calendar.MINUTE, 0);
                    retry = true;
                }
                if (d[2] == 1) next.add(Calendar.WEEK_OF_YEAR, 1);
            }

//            if (onlyWorkingDays) {
//                HolidayProviderIfc holidayProvider = M.l(HolidayProviderIfc.class);
//                if (holidayProvider != null) {
//                    while (!holidayProvider.isWorkingDay(null, next.getTime())) {
//                        next.add(Calendar.DAY_OF_MONTH, 1);
//                        next.set(Calendar.HOUR, 0);
//                        next.set(Calendar.MINUTE, 0);
//                        retry = true;
//                    }
//                }
//            }
        }
        return next.getTimeInMillis();
    }

    private int[] findNextAllowed(int[] allowed, int current) {
        int i = 0;
        if (allowed == null || allowed.length == 0) return new int[] {i, current, 0};
        for (int a : allowed) {
            if (a >= current) return new int[] {i, a, 0};
            i++;
        }
        return new int[] {0, allowed[0], 1};
    }

    @Override
    public String toString() {
        return definition;
    }

    public boolean isDisabled() {
        return disabled;
    }
}
