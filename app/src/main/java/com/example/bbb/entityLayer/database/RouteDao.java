package com.example.bbb.entityLayer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.bbb.entityLayer.data.POI;
import com.example.bbb.entityLayer.data.Route;

import java.util.List;

@Dao
public interface RouteDao {

    @Query("SELECT * FROM Route")
    List<Route> getAll();

    @Query("SELECT * FROM POI_ROUTE, POI WHERE POI_ROUTE.RouteID = :routeID AND POI.ID = POI_Route.PoiID")
    List<POI> getPOIs(int routeID);

    @Query("SELECT * FROM Route WHERE ID = :routeID")
    Route getRoute(int routeID);

    //add new queries here
    //@Query("QUERY")
    //method name + method type

    @Query("DELETE FROM Route")
    void deleteTable();

    @Insert
    void insertAll(List<Route> routes);
}
