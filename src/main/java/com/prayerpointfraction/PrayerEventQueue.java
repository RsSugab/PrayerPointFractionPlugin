package com.prayerpointfraction;

import lombok.Getter;
import lombok.Setter;

import java.util.Queue;

public class PrayerEventQueue
{
    @Getter
    @Setter
    private PrayerPointFractionPlugin.PrayerType prayerType;

    @Getter
    @Setter
    private boolean isPrayerActivated;

    PrayerEventQueue(PrayerPointFractionPlugin.PrayerType prayerType, boolean isPrayerActivated)
    {
        this.prayerType = prayerType;
        this.isPrayerActivated = isPrayerActivated;
    }
}
