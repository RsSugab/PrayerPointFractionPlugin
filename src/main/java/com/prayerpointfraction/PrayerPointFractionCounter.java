/*
 * Copyright (c) 2017, Tyler <https://github.com/tylerthardy>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.prayerpointfraction;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;

import java.awt.Color;
import java.awt.image.BufferedImage;

class PrayerPointFractionCounter extends Counter
{
    int prayerDrainThreshold;
    int prayerDrainCounter;
    boolean flagMissedPrayerFlick;
    boolean flagPrayerDrainCounterNotInitialized;

    PrayerPointFractionCounter(BufferedImage img, Plugin plugin, int amount, int prayerDrainThreshold, int prayerDrainCounter, boolean flagMissedPrayerFlick)
    {
        super(img, plugin, amount);
        this.prayerDrainThreshold = prayerDrainThreshold;
        this.prayerDrainCounter = prayerDrainCounter;
        this.flagMissedPrayerFlick = flagMissedPrayerFlick;
        this.flagPrayerDrainCounterNotInitialized = true;
    }

    @Override
    public String getTooltip()
    {
        if (flagPrayerDrainCounterNotInitialized)
        {
            return "Pray to an altar, use a POH pool or use a Falador Shield to initialize";
        }

        return "Prayer threshold: " + prayerDrainThreshold + "</br>"
                + "Prayer Drain Counter: " + prayerDrainCounter;
    }

    @Override
    public String getText()
    {
        if (flagPrayerDrainCounterNotInitialized)
        {
            return "?";
        }

        return super.getText();
    }

    @Override
    public Color getTextColor()
    {
        if (flagMissedPrayerFlick)
        {
            return Color.RED.brighter();
        }

        return Color.WHITE;
    }

    public void setFlag(boolean flag)
    {
        this.flagMissedPrayerFlick = flag;
    }

    public void initialize()
    {
        this.flagPrayerDrainCounterNotInitialized = false;
    }
}
