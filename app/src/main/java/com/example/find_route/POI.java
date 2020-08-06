package com.example.find_route;

/**
 * Created by 천도희 on 2017-09-05.
 */
import com.skp.Tmap.TMapPOIItem;

public class POI {
    TMapPOIItem item;

    public POI(TMapPOIItem item){
        this.item = item;
    }

    @Override
    public String toString() {
        return item.getPOIName();
    }
}



