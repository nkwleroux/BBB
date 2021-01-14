package com.example.bbb.controlLayer.gps;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;

import com.example.bbb.R;
import com.example.bbb.boundaryLayer.ui.BBBViewmodel;
import com.example.bbb.boundaryLayer.ui.IPOIVistitedListener;
import com.example.bbb.boundaryLayer.ui.IMarkerClickListener;
import com.example.bbb.boundaryLayer.ui.POIFragment;
import com.example.bbb.boundaryLayer.ui.UIViewModel;
import com.example.bbb.controlLayer.DatabaseManager;
import com.example.bbb.entityLayer.data.POI;
import com.example.bbb.entityLayer.data.Route;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenRouteService implements IMarkerClickListener, IPOIVistitedListener {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String API_KEY = "5b3ce3597851110001cf6248cc7335a16be74902905bcba4a9d0eebf";

    private final OkHttpClient client;
    private final OpenStreetMaps openStreetMaps;
    private final MapView mapView;
    private final View view;
    private final Context context;
    private ArrayList<GeoPoint> routeGeoPoints;
    private UIViewModel viewModel;
    private FragmentManager manager;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OpenRouteService(MapView mapView, Context context, View view, UIViewModel viewModel, FragmentManager manager) {
        this.client = new OkHttpClient();
        this.openStreetMaps = new OpenStreetMaps();
        this.mapView = mapView;
        this.view = view;
        this.context = context;
        this.viewModel = viewModel;
        this.manager = manager;
        BBBViewmodel.getInstance().setIpoiVistitedListener(this);
        this.routeGeoPoints = new ArrayList<>();
    }

    public ArrayList<GeoPoint> getRouteGeoPoints() {
        return routeGeoPoints;
    }

    public void setRouteGeoPoints(ArrayList<GeoPoint> routeGeoPoints) {
        this.routeGeoPoints = routeGeoPoints;
    }

    //Gets a route with more than 2 WayPoints.
    public void getRoute(double[][] wayPoints, String method, String language, Route route) {
        client.newCall(createPostRequest(method, getJsonString(wayPoints, language)))
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.d("FAILURE", "In OnFailure() in getRoute() multiple");
                    }

                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        try {
                            ArrayList<GeoPoint> points = new ArrayList<>();
                            JSONObject responseObject = new JSONObject(response.body().string());
                            JSONArray routesArray = responseObject.getJSONArray("routes");
                            JSONObject routes = (JSONObject) routesArray.get(0);
                            String geometry = routes.getString("geometry");
                            JSONArray coordinates = decodeGeometry(geometry, false);

                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray cordArray = (JSONArray) coordinates.get(i);
                                double lat = cordArray.getDouble(0);
                                double lng = cordArray.getDouble(1);
                                GeoPoint point = new GeoPoint(lat, lng);
                                points.add(point);
                            }

                            drawRouteWithMarkers(wayPoints, route);
                            openStreetMaps.drawRoute(mapView, points);
                            setRouteGeoPoints(points);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    static JSONArray decodeGeometry(String encodedGeometry, boolean inclElevation) {
        JSONArray geometry = new JSONArray();
        int len = encodedGeometry.length();
        int index = 0;
        int lat = 0;
        int lng = 0;
        int ele = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedGeometry.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedGeometry.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);


            if (inclElevation) {
                result = 1;
                shift = 0;
                do {
                    b = encodedGeometry.charAt(index++) - 63 - 1;
                    result += b << shift;
                    shift += 5;
                } while (b >= 0x1f);
                ele += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            }

            JSONArray location = new JSONArray();
            try {
                location.put(lat / 1E5);
                location.put(lng / 1E5);
                if (inclElevation) {
                    location.put((float) (ele / 100));
                }
                geometry.put(location);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return geometry;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drawRouteWithMarkers(double[][] waypoints, Route route) {
        for (int i = 0; i < waypoints.length; i++) {
            if (view == null) {
                return;
            }

            if (view != null) {
                if (i != 0 && i != waypoints.length - 1) {
                    openStreetMaps.drawMarker(
                            mapView, new GeoPoint(waypoints[i][1], waypoints[i][0]),
                            context.getDrawable(R.drawable.waypoint_unvisited),
                            context.getDrawable(R.drawable.waypoint_visited),
                            DatabaseManager.getInstance().getPOIsFromRoute(route.ID).get(i),
                            OpenRouteService.this);
                } else if (i == 0) {
                    openStreetMaps.drawMarker(
                            mapView, new GeoPoint(waypoints[i][1], waypoints[i][0]),
                            context.getDrawable(R.drawable.start_location),
                            DatabaseManager.getInstance().getPOIsFromRoute(route.ID).get(i),
                            OpenRouteService.this);
                } else {
                    openStreetMaps.drawMarker(
                            mapView, new GeoPoint(waypoints[i][1], waypoints[i][0]),
                            context.getDrawable(R.drawable.end_location),
                            DatabaseManager.getInstance().getPOIsFromRoute(route.ID).get(i),
                            OpenRouteService.this);
                }
            }

        }

        if (view == null) {
            return;
        }
        openStreetMaps.drawRoute(mapView, getRouteGeoPoints());
    }

    private String getJsonString(double[][] wayPoints, String language) {
        return "{\"coordinates\":" + Arrays.deepToString(wayPoints) + ",\"language\":\"" + language + "\"}";
    }

    public Request createPostRequest(String method, String json) {
        RequestBody requestBody = RequestBody.create(json, JSON);
        return new Request.Builder().url("https://api.openrouteservice.org/v2/directions/" + method).
                post(requestBody).addHeader("Authorization", API_KEY).build();
    }


    @Override
    public void onMarkerClicked(POI poi) {
        viewModel.setSelectedPOI(poi);
        manager.beginTransaction().replace(R.id.fragment_container,
                new POIFragment()).addToBackStack(null).commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPoiIsVisited(POI poi) {
        openStreetMaps.drawMarker(mapView, new GeoPoint(poi.longitude, poi.latitude),
                context.getDrawable(R.drawable.waypoint_visited), poi, this);
        System.out.println("@@@@@@@ on POI is Visited");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void drawMarkers(){
        for (POI poi : viewModel.getPOIs().getValue()){
            openStreetMaps.drawMarker(mapView, new GeoPoint(poi.longitude, poi.latitude), context.getDrawable(R.drawable.waypoint_unvisited), poi, this);
        }
    }
}

