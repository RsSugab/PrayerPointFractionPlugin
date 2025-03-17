package com.prayerpointfraction;

import lombok.Getter;
import lombok.Setter;

import java.util.Queue;

public class PrayerEventQueue
{
    @Getter
    @Setter
    private int varbitId;

    @Getter
    @Setter
    private boolean isPrayerActivated;

    PrayerEventQueue(int varbitId, boolean isPrayerActivated)
    {
        this.varbitId = varbitId;
        this.isPrayerActivated = isPrayerActivated;
    }
}
