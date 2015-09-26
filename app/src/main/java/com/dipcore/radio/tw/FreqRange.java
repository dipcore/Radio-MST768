package com.dipcore.radio.tw;

public class FreqRange {
    public int minFreq;
    public int maxFreq;
    public int step;
    public int id; // TWUtil arg2: 0 - fm1, 1- fm2, 2 - am
    public String units = "MHz"; // frequency units

    FreqRange(int aId){
        id = aId;
    }
}
