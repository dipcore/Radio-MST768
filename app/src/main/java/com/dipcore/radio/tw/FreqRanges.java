package com.dipcore.radio.tw;

import java.util.HashMap;

public class FreqRanges {

    private HashMap<String, FreqRange> ranges = new HashMap<String, FreqRange>();

    public HashMap<String, FreqRange> get(){
        return ranges;
    }

    public FreqRange get(String key){
        return ranges.get(key);
    }

    public FreqRange getById(int id){

        FreqRange result = null;

        for (HashMap.Entry<String, FreqRange> entry : ranges.entrySet()) {
            FreqRange freqRange = entry.getValue();
            if (freqRange.id == id){
                result = freqRange;
            }
        }

        return result;
    }


    public FreqRange add(String key, int id){
        return add(key, id, "MHz");
    }

    public FreqRange add(String key, int id, String units){
        FreqRange freqRange = new FreqRange(id);
        freqRange.units = units;
        ranges.put(key, freqRange);
        return freqRange;
    }


}
