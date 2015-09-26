package com.dipcore.radio.tw;

import android.os.Handler;

import android.tw.john.TWUtil;

import java.util.ArrayList;
import java.util.List;

public class Radio {

    /**
     * Interface
     */
    public interface NoticeListener {
        void onFrequencyChanged(int freq);
        void onFreqRangeChanged(FreqRange freqRange);
        void onFreqRangeParamsReceived();
        void onFlagChangedTA(boolean flag);
        void onFlagChangedREG(boolean flag);
        void onFlagChangedAF(boolean flag);
        void onFlagChangedLOC(boolean flag);
        void onFlagChangedRDSTP(boolean flag);
        void onFlagChangedRDSTA(boolean flag);
        void onFlagChangedRDSST(boolean flag);
        void onFlagChangedScanning(boolean flag);
        void onStationFound(int number, int freq, String name);
        void onFoundPTY(int Id, int requestedId);
        void onFoundRDSPSText(String text);
        void onFoundRDSText(String text);
        void onSetRegionId(int id);
        void onKeyPress(int code, int duration);
    }

    /**
     * Variables
     */

    private boolean scanningFlag = false;
    public String[] PTYNames = { "None", "News", "Affairs", "Info", "Sport", "Educate", "Drama", "Culture", "Science", "Varied", "Pop Music", "Rock Music", "Easy Music", "Light Music", "Classics", "Other Music", "Weather", "Finance", "Children", "Social", "Religion", "Phone In", "Trave ", "Leisure", "Jazz", "Country", "Nation M", "Oldies", "Folk Music", "Document", "Test", "Alarm" };
    public FreqRange freqRange;
    public FreqRanges freqRanges;

    public int freq;
    public int statusRegisterB = 0;
    public int statusRegisterA = 0;
    public int region = 0;
    public boolean audioFocus = false;
    public boolean flagAF = false;

    private int freqAfterFreqRangeChanged = -1; // TODO Use promises

    private Handler handler = new RadioHandler(this);

    private List<NoticeListener> listeners = new ArrayList<>();

    public  TWUtil twUtil;


    public Radio(){
        // Ranges
        freqRanges = new FreqRanges();
        freqRanges.add("FM", 0, "MHz"); // FM1 - 0, FM2 - 1
        freqRanges.add("AM", 2, "KHz"); // AM - 2


        // DEVEL
        freqRanges.get("FM").minFreq = 8750;
        freqRanges.get("FM").maxFreq = 10800;
        freqRanges.get("FM").step = 10;

        freqRanges.get("AM").minFreq = 8175;
        freqRanges.get("AM").maxFreq = 11337;
        freqRanges.get("AM").step = 5;

        freqRange = freqRanges.get("FM");
        // End DEVEL


    }

    /**
     *
     * Radio public methods
     *
     */

    /**
     * Init radio
     */

    public void init(){

        if (twUtil != null)
            return;

        twUtil = new TWUtil(1); // Radio
        short[] initialSequence =  new  short[] { 265, 513, 769, 1025, 1026, 1028, 1029, 1030 };
        if (twUtil.open(initialSequence) == 0) {
            twUtil.start();
            twUtil.addHandler("radio", handler);

            twUtil.write(265, 255); // Settings (region)
            twUtil.write(769, 255);
            twUtil.write(1030, 0);
            twUtil.write(1025, 255);
            twUtil.write(1028, 255);
            twUtil.write(1029, 255);

            queryAudioFocus();
        } else {
            // finish();
        }
    }

    /**
     * Stop radio
     */
    public void stop(){
        releaseAudioFocus();
        twUtil.stop();
        twUtil.close();
        twUtil = null;
    }

    /**
     * Pause
     */
    public void pause(){
        //twUtil.write(40448, 129); // ??
        //twUtil.removeHandler("radio");
        releaseAudioFocus();
    }

    /**
     * Resume
     */
    public void resume(){
        //init();
        queryAudioFocus();
    }

    /**
     * Set radio region
     * @param id
     */
    public void setRegion(int id){
        twUtil.write(265, 0, id);
    }

    /**
     * Set frequency range by its name
     * @param aName (String)
     * @return FreqRange
     */
    public FreqRange setFreqRangeByName(String aName){
            freqRange = freqRanges.get(aName);
            twUtil.write(1025, 5, freqRange.id);
            return freqRange;
    }

    /**
     * Set frequency range by its id
     * @param aId (String)
     * @return FreqRange
     */
    public FreqRange setFreqRangeById(int aId){
            freqRange = freqRanges.getById(aId);
            twUtil.write(1025, 5, freqRange.id);
            return freqRange;
    }

    /**
     * Request current frequency range parameters (min, max, step etc)
     */
    public void getFreqRangeParams(){
         twUtil.write(1030, 1);
    }

    /**
     * Set frequency
     * @param aFreq (int)
     */
    public void setFreq(int aFreq) {
        if (freq != aFreq) {
            if (aFreq > freqRange.maxFreq) {
                aFreq = freqRange.maxFreq;
            }
            if (aFreq < freqRange.minFreq) {
                aFreq = freqRange.minFreq;
            }
            freq = aFreq;
            twUtil.write(1026, 255, aFreq);
        } else {
            frequencyChanged(freq);
        }
    }

    /**
     * Set station
     * Used to set range and frequency
     * @param aFreq
     */
    public void setStation(int aFreqRangeId, int aFreq){
        if (freqRange == null || aFreqRangeId != freqRange.id) {
            setFreqRangeById(aFreqRangeId);
            freqAfterFreqRangeChanged = aFreq;
        } else {
            setFreq(aFreq);
            freqAfterFreqRangeChanged = -1;
        }
    }

    /**
     * Start auto-scan
     */
    public void autoScan(){
        twUtil.write(1025, 0, 0);
    }

    /**
     * Toggle REG flag
     */
    public void toggleREGFlag(){
        boolean flag = !((statusRegisterB & 0x4) == 0x4);
        setREGFlag(flag);
    }

    /**
     * Set REG flag
     * @param flag
     */
    public void setREGFlag(boolean flag) {
        twUtil.write(1028, 3, (flag ? 1 : 0));
    }

    /**
     * Toggle TA flag
     */
    public void toggleTAFlag(){
        boolean flag = !((statusRegisterB & 0x20) == 0x20);
        setTAFlag(flag);
    }

    /**
     * Set TA flag
     * @param flag
     */
    public void setTAFlag(boolean flag) {
        twUtil.write(1028, 1, (flag ? 1 : 0));
    }

    /**
     * Toggle LOC flag
     */
    public void toggleLOCFlag(){
        boolean flag = !((statusRegisterA & 0x8) == 0x8);
        setLOCFlag(flag);
    }

    /**
     * Set DX flag
     * @param flag
     */
    public void setLOCFlag(boolean flag) {
        twUtil.write(1025, 4, (flag ? 1 : 0));
    }

    /**
     * Toggle AF flag
     */
    public void toggleAFFlag(){
        boolean flag = !((statusRegisterB & 0x40) == 0x40);
        setAFFlag(flag);
    }

    /**
     * Set AF flag
     * @param flag
     */
    public void setAFFlag(boolean flag) {
        twUtil.write(1028, 0, (flag ? 1 : 0));
    }


    /**
     * Query audio focus for radio
     */
    public void queryAudioFocus(){
        audioFocus = true;
        twUtil.write(769, 192, 1);
        twUtil.write(40465, 192, 1);
    }

    /**
     * Release audio focus for radio
     */
    public void releaseAudioFocus(){
        audioFocus = false;
        twUtil.write(769, 192, 0);
        twUtil.write(40465, 192, 129);
    }

    /**
     * Get audio focus flag
     * @return
     */
    public boolean isAudioFocus() {
        return audioFocus;
    }

    /**
     * Seek next station
     */
    public void seekNextStation(){
        twUtil.write(1025, 1, 0);
    }

    /**
     * Seek prev station
     */
    public void seekPrevStation(){
        twUtil.write(1025, 1, 1);
    }


    /**
     *
     * Notice Listener methods (callbacks)
     *
     */

    /**
     * Add listener
     * @param listener
     */
    public void addListener(NoticeListener listener) {
        listeners.add(listener);
    }

    /**
     * Frequency changed (RadioHandler)
     * @param aFreq
     */
    public void frequencyChanged(int aFreq){
        freq = aFreq;
        for(NoticeListener listener : listeners){
            listener.onFrequencyChanged(aFreq);
        }
    }

    /**
     * Frequency Range Changed (RadioHandler)
     * @param aRangeId
     */
    public void freqRangeChanged(int aRangeId){
        // We do not have FM2
        if (aRangeId == 1){
            aRangeId = 0;
        }
        if (freqAfterFreqRangeChanged > 0) {
            setFreq(freqAfterFreqRangeChanged);
            freqAfterFreqRangeChanged = -1;
        }
        freqRange = freqRanges.getById(aRangeId);
        for(NoticeListener listener : listeners){
            listener.onFreqRangeChanged(freqRange);
        }
    }

    /**
     * Frequency Range Changed (RadioHandler)
     */
    public void freqRangesPramsReceived(){
        if (freqRange != null)
            for(NoticeListener listener : listeners){
                listener.onFreqRangeParamsReceived();
            }
    }

    /**
     * TA changed
     * @param flag
     */
    public void flagChangedTA(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedTA(flag);
        }
    }

    /**
     * REG changed
     * @param flag
     */
    public void flagChangedREG(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedREG(flag);
        }
    }

    /**
     * AF changed
     * @param flag
     */
    public void flagChangedAF(boolean flag){
        flagAF = flag;
        for(NoticeListener listener : listeners){
            listener.onFlagChangedAF(flag);
        }
    }

    /**
     * DX changed (DX/LOC)
     * @param flag
     */
    public void flagChangedDX(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedLOC(flag);
        }
    }

    /**
     * RDS_TP changed
     * @param flag
     */
    public void flagChangedRDSTP(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedRDSTP(flag);
        }
    }

    /**
     * RDS_TA changed
     * @param flag
     */
    public void flagChangedRDSTA(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedRDSTA(flag);
        }
    }

    /**
     * PTY Id found
     * PTY scanning use: twUtil(1028, 4, PTY_Id_To_find);
     * @param PTYId
     * @param requestedPTYId
     */
    public void foundPTY(int PTYId, int requestedPTYId){
        for(NoticeListener listener : listeners){
            listener.onFoundPTY(PTYId, requestedPTYId);
        }
    }

    /**
     * RDS_PS text found
     * a part of the previous one
     * @param text
     */
    public void foundRDSPSText(String text){
        for(NoticeListener listener : listeners){
            listener.onFoundRDSPSText(text);
        }
    }

    /**
     * RDS long message
     * @param text
     */
    public void foundRDSText(String text){
        for(NoticeListener listener : listeners){
            listener.onFoundRDSText(text);
        }
    }

    /**
     * RDS_ST changed
     * @param flag
     */
    public void flagChangedRDSST(boolean flag){
        for(NoticeListener listener : listeners){
            listener.onFlagChangedRDSST(flag);
        }
    }

    /**
     * Scanning flag changed
     * @param flag
     */
    public void flagChangedScanning(boolean flag){
        scanningFlag = flag;
        for(NoticeListener listener : listeners){
            listener.onFlagChangedScanning(flag);
        }
    }

    /**
     * Station found (scanning)
     * @param aNumber
     * @param aFreq
     * @param aName
     */
    public  void stationFound(int aNumber, int aFreq, String aName){
        if (scanningFlag && aName == null || flagAF)
            for(NoticeListener listener : listeners){
                listener.onStationFound(aNumber, aFreq, aName);
            }
    }

    /**
     * Received region id
     * @param id
     */
    public void setRegionId(int id){
        for(NoticeListener listener : listeners){
            listener.onSetRegionId(id);
        }
    }

    /**
     * Key pressed
     * @param code
     * @param duration
     */
    public void keyPressed(int code, int duration){
        for(NoticeListener listener : listeners){
            listener.onKeyPress(code, duration);
        }
    }
}
