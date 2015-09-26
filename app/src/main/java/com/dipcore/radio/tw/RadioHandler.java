package com.dipcore.radio.tw;

import android.os.Handler;
import android.os.Message;


class RadioHandler extends Handler {

    final Radio radio;

    RadioHandler(Radio aRadio){
        radio = aRadio;
    }

    private boolean enabled = false;

    public void handleMessage(Message message) {

        try {
            //System.out.println("WHAT: " + message.what);
            //System.out.println("ARG1: " + message.arg1);
            //System.out.println("ARG2: " + message.arg2);
            //if (message.what != 265)
            //    System.out.println("OBJ: " + message.obj);
        } catch (Error e) {

        }


        if (message.what == 769) {
            // arg1, Audio focus: 0 - released, 1- focused, 3 - pause
            enabled = message.arg1 == 1;
            switch (message.arg1) {
                case 0:
                    //radio.queryAudioFocus();
                    break;
                case 1:
                    //radio.queryAudioFocus();
                    break;
                case 3:
                    //radio.queryAudioFocus();
                    break;
            }

        }

        if (true) {
            switch (message.what) {
                case 513:
                    // message.arg1 = 1 - short
                    // message.arg1 = 2 - long
                    if (enabled)
                        radio.keyPressed(message.arg2, message.arg1);
                    break;
                case 265:
                    // Radio region
                    byte[] array = (byte[]) message.obj;
                    int regionId = array[0];
                    radio.setRegionId(regionId);
                    break;
                case 1025:
                    switch (message.arg1) {
                        case 0:

                            //Tools.printInfo1025(message.arg2);

                            if (radio.statusRegisterA != message.arg2) {

                                final int updatedBits = radio.statusRegisterA ^ message.arg2; // XOR, select changed bits only
                                radio.statusRegisterA = message.arg2;

                                if ((updatedBits & 0x1) == 0x1) {
                                    boolean flag = ((radio.statusRegisterA & 0x1) == 0x1);
                                }
                                if ((updatedBits & 0x2) == 0x2) {
                                    boolean flag = ((radio.statusRegisterA & 0x2) == 0x2);
                                }
                                if ((updatedBits & 0x4) == 0x4) {
                                    boolean flag = ((radio.statusRegisterA & 0x4) == 0x4);
                                }

                                // DX/LOC
                                if ((updatedBits & 0x8) == 0x8) {
                                    boolean flag = ((radio.statusRegisterA & 0x8) == 0x8);
                                    radio.flagChangedDX(flag);
                                }
                                // Stereo flag (RDS_ST)
                                if ((updatedBits & 0x10) == 0x10) {
                                    boolean flag = ((radio.statusRegisterA & 0x10) == 0x10);
                                    radio.flagChangedRDSST(flag);
                                }
                                if ((updatedBits & 0x20) == 0x20) {
                                    boolean flag = ((radio.statusRegisterA & 0x20) == 0x20);
                                }
                                // Seek station flag (1 - started, 0 - ended)
                                if ((updatedBits & 0x40) == 0x40) {
                                    boolean flag = ((radio.statusRegisterA & 0x40) == 0x40);
                                }
                                // 0b10000000 Scanning flag
                                if ((updatedBits & 0x80) == 0x80) {
                                    boolean flag = ((radio.statusRegisterA & 0x80) == 0x80); // Scanning started - 1, Scanning ended - 0
                                    radio.flagChangedScanning(flag);
                                }
                            }
                            break;
                        case 1:
                            int rangeId = message.arg2; // Band id: 0 - fm1, 1 - fm2, 2 - am
                            radio.freqRangeChanged(rangeId);
                            break;
                        case 2:
                            int freq = message.arg2; // Frequency
                            radio.frequencyChanged(freq);
                            String stationName = String.valueOf(message.obj); // Station name, from TWUtil
                            break;
                        case 3:
                            int PTYNumber = message.arg2; // PTY group number
                            break;
                        case 4:
                            int stationNumber = message.arg2; // Station number
                            break;
                        default:
                            break;
                    }
                    break;
                case 1026:

                    // On loading, if twUtil(1025, 255) it returns list of stored stations (twUtil)
                    // On scanning it contains found station details

                    boolean scanning = ((radio.statusRegisterA & 0x80) == 0x80);
                    int number = message.arg1;
                    int freq = message.arg2;
                    String name = (String) message.obj;

                    if (scanning) {
                        radio.stationFound(number, freq, name);
                    } else {
                        // Stored station (on load)
                    }

                    // HERE SHOULD BE PTY SCANNING RESULTS TOO ??
                    break;
                case 1028:

                    //Tools.printInfo1028(message.arg1);

                    if (radio.statusRegisterB != message.arg1 || true) {

                        final int n6 = radio.statusRegisterB ^ message.arg1; // XOR
                        radio.statusRegisterB = message.arg1;

                        if ((n6 & 0x1) == 0x1) { // ??
                            boolean flag = ((radio.statusRegisterB & 0x1) == 0x1);
                            //System.out.println("0x1 ?? " + flag);
                        }
                        if ((n6 & 0x2) == 0x2) { // RDS_TA
                            boolean flag = ((radio.statusRegisterB & 0x2) == 0x2);
                            radio.flagChangedRDSTA(flag);
                            //System.out.println("0x2 RDS_TA " + flag);
                        }
                        if ((n6 & 0x4) == 0x4) { // REG
                            boolean flag = ((radio.statusRegisterB & 0x4) == 0x4);
                            radio.flagChangedREG(flag);
                            //System.out.println("0x4 REG " + flag);
                        }
                        if ((n6 & 0x8) == 0x8) { // ?? works with twUtil(1028, 2, 0|1)
                            boolean flag = ((radio.statusRegisterB & 0x8) == 0x8);
                            //System.out.println("0x8 ??? " + flag);
                        }
                        if ((n6 & 0x10) == 0x10) { // ??
                            boolean flag = ((radio.statusRegisterB & 0x10) == 0x10);
                            //System.out.println("0x10 ??? " + flag);
                        }
                        if ((n6 & 0x20) == 0x20) { // TA
                            boolean flag = ((radio.statusRegisterB & 0x20) == 0x20);
                            radio.flagChangedTA(flag);
                            //System.out.println("0x20 TA " + flag);
                        }
                        if ((n6 & 0x40) == 0x40) { // AF
                            boolean flag = ((radio.statusRegisterB & 0x40) == 0x40);
                            radio.flagChangedAF(flag);
                            //System.out.println("0x40 AF " + flag);
                        }
                        if ((n6 & 0x80) == 0x80) { // RDS_TP
                            boolean flag = ((radio.statusRegisterB & 0x80) == 0x80);
                            radio.flagChangedRDSTP(flag);
                            //System.out.println("0x80 RDS_TP " + flag);
                        }
                    }

                    int PTYId = 0xFF & message.arg2;
                    int requestedPTYId = 0xFF & message.arg2 >> 8;


                    // Short RDS text
                    String RDSPSText = ((String) message.obj).trim();
                    radio.foundRDSPSText(RDSPSText);

                    // PTY id
                    radio.foundPTY(PTYId, requestedPTYId);



                    //System.out.println("requestedPTYId " + requestedPTYId); // On PTY search, twUtil(1028, 4, PTY_Id_To_find);
                    //System.out.println("PTYId " + PTYId);
                    break;
                case 1029: // RDS text message
                    //System.out.println("OBJ: " + message.obj);
                    String msg = (String) message.obj;
                    radio.foundRDSText(msg);
                    break;
                case 1030: // AM, FM ranges
                    switch (message.arg1) {
                        case 0: // Region
                            radio.region = message.arg2; // 0 - CHINA, 1 - EUROPE, 2 - USA, 3 - SOUTHEAST ASIA, 4 - SOUTH AMERICA, 5 - EASTERN EUROPE, 6 - JAPAN
                            break;
                        case 1: // FM max
                            radio.freqRanges.get("FM").maxFreq = message.arg2;
                            break;
                        case 2: // FM min
                            radio.freqRanges.get("FM").minFreq = message.arg2;
                            break;
                        case 3: // AM max
                            radio.freqRanges.get("AM").maxFreq = message.arg2;
                            break;
                        case 4: // AM min
                            radio.freqRanges.get("AM").minFreq = message.arg2;
                            break;
                        case 5: // FM step
                            radio.freqRanges.get("FM").step = message.arg2;
                            break;
                        case 6: // AM step
                            radio.freqRanges.get("AM").step = message.arg2;
                            break;
                        case 7: // ??
                            //if (radio.region != 5)
                                radio.freqRangesPramsReceived();
                            break;
                        case 8: // FM2 max
                            //radio.freqRanges.get("FM").maxFreq = message.arg2;
                            break;
                        case 9: // FM2 min
                            //radio.freqRanges.get("FM").minFreq = message.arg2;
                            break;
                        case 10: // FM2 step
                            //radio.freqRanges.get("AM").step = message.arg2;
                            //if (radio.region != 5)
                            //radio.freqRangesPramsReceived();
                            break;

                    }
                    break;
            }

        } // endif
    }
}
