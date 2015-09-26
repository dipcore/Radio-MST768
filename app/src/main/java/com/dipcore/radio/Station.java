package com.dipcore.radio;

import java.util.UUID;

public class Station {

    public String uuid;
    public String name;
    public int freq;
    public int freqRangeId = 0;
    public boolean favorite = false;

    Station(String aName, int aFreq){
        name = aName;
        freq = aFreq;
    }

    Station(String aName, int aFreq, int aFreqRangeId) {
        name = aName;
        freq = aFreq;
        freqRangeId = aFreqRangeId;
        uuid = UUID.randomUUID().toString();
    }


    Station(String aName, int aFreq, int aFreqRangeId, String aUUID) {
        name = aName;
        freq = aFreq;
        freqRangeId = aFreqRangeId;
        uuid = aUUID;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
}