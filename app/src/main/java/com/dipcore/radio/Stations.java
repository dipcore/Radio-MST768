package com.dipcore.radio;

import java.util.ArrayList;
import java.util.Iterator;

public class Stations extends ArrayList<Station> {

    public Station findByFreq(int freq){
        Station result = null;
        for (Station station : this) {
            if (station.freq == freq){
                result = station;
            }
        }
        return result;
    }

    public int findIdByFreq(int freq){
        int id = 0;
        int result = -1;
        for (Station station : this) {
            if (station.freq == freq){
                result = id;
            }
            id ++;
        }
        return result;
    }

    public void setFavorite(int index, boolean flag){
        int id = 0;
        for (Station station : this) {
            if (id == index){
                this.get(id).setFavorite(flag);
            }
            id ++;
        }
    }

    public void removeByUUID(String uuid){
        Iterator<Station> it = this.iterator();
        while (it.hasNext()) {
            Station station = it.next();
            if (station.uuid.equals(uuid)) {
                it.remove();
            }
        }
    }

    public void setFavoriteByUUID(String uuid, boolean flag){
        int id = 0;
        for (Station station : this) {
            if (station.uuid.equals(uuid)){
                this.get(id).setFavorite(flag);
            }
            id ++;
        }
    }

    public void setByUUID(String uuid, Station aStation){
        int id = 0;
        for (Station station : this) {
            if (station.uuid.equals(uuid)){
                this.set(id, aStation);
            }
            id ++;
        }
    }

    public Station getByUUID(String uuid){
        Station result = null;
        for (Station station : this) {
            if (station.uuid.equals(uuid)){
                return station;
            }
        }
        return result;
    }

    public int idByUUID(String uuid){
        int id = 0;
        for (Station station : this) {
            if (station.uuid.equals(uuid)){
                return id;
            }
            id ++;
        }
        return id;
    }

    public void clearFavorites(){
        int id = 0;
        for (Station station : this) {
               this.get(id).setFavorite(false);
            id ++;
        }
    }
}