package com.example.bbb.boundaryLayer.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.bbb.R;
import com.example.bbb.controlLayer.gps.OpenRouteService;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {
    private Context fragmentContext;
    private IMapController mapController;
    private MapView map;
    private OpenRouteService openRouteService;
    private Marker currentLocation;
    private ImageButton ibRouteInfo;
    private ImageButton ibHelpPopup;
    private ImageButton ibUserInfo;
    private Fragment userInfoFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentContext = getActivity().getApplicationContext();
        Configuration.getInstance().load(fragmentContext, PreferenceManager.getDefaultSharedPreferences(fragmentContext));

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = (MapView) view.findViewById(R.id.map_view);
        map.setUseDataConnection(true);
        map.setTileSource(TileSourceFactory.MAPNIK);

        mapController = map.getController();
        mapController.setZoom(14);

        //Check for Location permission
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
        }

        //Zoom in with pinching
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);

        openRouteService = new OpenRouteService(map);

        ibRouteInfo = view.findViewById(R.id.imageButtonRouteInfo);
        ibHelpPopup = view.findViewById(R.id.imageButtonHelp);
        ibUserInfo = view.findViewById(R.id.imageButtonUserInfo);
        buttonClickListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getLocation();

        openRouteService.getRoute(new GeoPoint[]{
                new GeoPoint(51.813297, 4.690093),
                new GeoPoint(49.41943,8.686507),
                new GeoPoint(49.420318,8.687872)
        }, "driving-car", "de");
    }

    public void getLocation() {
        LocationManager locationManager = (LocationManager) fragmentContext.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = location -> {
            if (getView() == null){
                return;
            }
            Log.d("Latitude", "onLocationChanged: " + location.getLatitude());
            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setCenter(point);
            Marker startPoint = new Marker(map);
            startPoint.setPosition(point);
            map.getOverlays().remove(currentLocation);
            currentLocation = startPoint;
            map.getOverlays().add(startPoint);
        };

        if(ContextCompat.checkSelfPermission(fragmentContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
        }
    }

    public void buttonClickListeners(){
        ibRouteInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialogFragment = new RoutePopUp();
                dialogFragment.show(getActivity().getSupportFragmentManager(), "route_popup");
            }
        });

        ibHelpPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO laterrrr
            }
        });

        ibUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUserInfoFragment(getActivity().getSupportFragmentManager());
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, userInfoFragment).commit();
            }
        });
    }

    public void setUserInfoFragment(FragmentManager fm){
        if(fm.findFragmentById(R.id.user_info_fragment) == null){
            userInfoFragment = new UserInfoFragment();
        } else {
            userInfoFragment = (UserInfoFragment) fm.findFragmentById(R.id.user_info_fragment);
        }
    }
}
