/*
 * Copyright (c) 2014, Codename One LTD. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.googlemaps;

import android.Manifest;

import com.codename1.io.Util;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.codename1.impl.android.AndroidNativeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.os.Bundle;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.LifecycleListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;
import android.os.Looper;
import android.view.View;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.Polyline;
import android.graphics.Point;
//import com.codename1.impl.android.AndroidImplementation.PeerDraw;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.google.android.gms.maps.model.CameraPosition;

public class InternalNativeMapsImpl implements LifecycleListener {
    private int mapId;
    private MapView view;
    private GoogleMap mapInstance;
    private static int uniqueIdCounter = 0;
    private HashMap<Long, Marker> markerLookup = new HashMap<Long, Marker>();
    private HashMap<Marker, Long> listeners = new HashMap<Marker, Long>();
    private static boolean supported = true;
    private HashMap<Long, Polyline> paths = new HashMap<Long, Polyline>();
    private PolylineOptions currentPath;
    private LatLng lastPosition;
    private Point lastPoint;
    private boolean showMyLocation;
    private boolean rotateGestureEnabled;

    static {
        if(AndroidNativeUtil.getActivity() != null) {
            android.util.Log.d("CN1 Maps", "Initializing maps");
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                initMaps();
            } else {
                AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        initMaps();
                    }
                });
            }
        } else {
            android.util.Log.d("CN1 Mapss", "Did not initialize maps because activity was null");
        }
        AndroidNativeUtil.registerViewRenderer(MapView.class, new AndroidNativeUtil.BitmapViewRenderer() {

            public Bitmap renderViewOnBitmap(View v, final int w, final int h) {

                PeerImage pi = PeerImage.getPeerImage(v);
                if (pi == null) {
                    PeerImage.submitUpdate(v, w, h);
                    return null;
                }
                if (pi.peerImage == null || pi.peerW != w || pi.peerH != h) {
                    PeerImage.submitUpdate(v, w, h);
                    return null;
                }
                pi.lastUsed = System.currentTimeMillis();
                return pi.peerImage;

            }
        });
    }

    private static class PeerImage {

        Bitmap peerImage;
        int peerW;
        int peerH;
        Timer timer;
        long lastUsed;


        PeerImage() {
            lastUsed = System.currentTimeMillis();
        }

        public static PeerImage getPeerImage(View v) {
            return peerImages.get(v);
        }

        public void update(View v, final int w, final int h) {
            // prevent potential exception during transitions
            if(w < 10 || h < 10) {

                return;
            }
            final MapView mv = (MapView)v;
            if(mv.getParent() == null || mv.getHeight() < 10 || mv.getWidth() < 10) {
                return;
            }
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    //mv.
                    mv.getMapAsync(new OnMapReadyCallback() {
                                       @Override
                                       public void onMapReady(GoogleMap googleMap) {
                                           googleMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
                                               public void onSnapshotReady(Bitmap snapshot) {
                                                   peerImage = snapshot;
                                                   peerW = w;
                                                   peerH = h;
                                                   lastUsed = System.currentTimeMillis();
                                               }
                                           });
                                       }
                                   });


                }

            });
        }

        private static PendingUpdate findPendingUpdate(View v) {
            synchronized(pendingUpdates) {
                for (PendingUpdate u : pendingUpdates) {
                    if (u.view == v) {
                        return u;
                    }
                }
            }
            return null;
        }

        private static void submitUpdate(View v, int w, int h) {
            synchronized(pendingUpdates) {
                PendingUpdate existing = findPendingUpdate(v);
                if (existing == null) {
                    existing = new PendingUpdate();
                    existing.view = v;
                    existing.w = w;
                    existing.h = h;
                    pendingUpdates.add(existing);
                    existing.schedule();
                }
                existing.w = w;
                existing.h = h;
            }

        }

        private static void clearOldest() {
            synchronized (peerImages) {
                int maxNum = 5;
                long currMark = System.currentTimeMillis();
                ArrayList<PeerImage> toRemove = new ArrayList<PeerImage>();
                while (peerImages.size() > 5) {
                    View oldest = null;
                    PeerImage oldestImg = null;
                    for (View vimg : peerImages.keySet()) {
                        PeerImage img = peerImages.get(vimg);
                        if (oldest == null || img.lastUsed < oldestImg.lastUsed) {
                            oldest = vimg;
                            oldestImg = img;
                        }
                    }
                    if (oldest != null) {
                        peerImages.remove(oldest);
                    }

                }

            }
        }

    }

    static HashMap<View, PeerImage> peerImages = new HashMap<View,PeerImage>();
    
    private static class PendingUpdate {
        private View view;
        private int w;
        private int h;
        long requestTime;
        Timer timer;

        private void schedule() {
            timer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    synchronized(pendingUpdates) {
                        pendingUpdates.remove(PendingUpdate.this);
                    }
                    PeerImage pe = peerImages.get(view);
                    if (pe == null) {
                        pe = new PeerImage();
                        peerImages.put(view, pe);
                    }
                    pe.update(view, w, h);

                }
            };
            timer.schedule(tt, 1000L);
        }
    }
    
    static java.util.ArrayList<PendingUpdate> pendingUpdates = new java.util.ArrayList<PendingUpdate>();


    private static boolean initialized = false;
    private static void initMaps() {
        if (!initialized) {
            initialized = true;
            try {
                // this triggers the creation of the maps so they are ready when the peer component is invoked
                MapsInitializer.initialize(AndroidNativeUtil.getActivity());
                MapView v = new MapView(AndroidNativeUtil.getActivity());

                v.onCreate(AndroidNativeUtil.getActivationBundle());
                v.onResume();
                //v.getMap();
            } catch (Exception e) {
                supported = false;
                System.out.println("Failed to initialize, google play services not installed: " + e);
                e.printStackTrace();
            }
        } 
    }
        
    private void setupMap() {
    }

    private void installListeners() {
        /*
        if (mapInstance == null) {
            view = null;
            System.out.println("Failed to get map instance, it seems google play services are not installed");
            return;
        }*/
        view.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mapInstance = googleMap;
                mapInstance.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    public boolean onMarkerClick(Marker marker) {
                        Long val = listeners.get(marker);
                        if (val != null) {
                            MapContainer.fireMarkerEvent(InternalNativeMapsImpl.this.mapId, val.longValue());
                            return true;
                        }
                        return false;
                    }
                });
                mapInstance.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    public void onCameraChange(CameraPosition position) {
                        MapContainer.fireMapChangeEvent(InternalNativeMapsImpl.this.mapId, (int) position.zoom, position.target.latitude, position.target.longitude);
                        PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                    }
                });
                mapInstance.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        MapContainer.fireMapChangeEvent(InternalNativeMapsImpl.this.mapId, (int) mapInstance.getCameraPosition().zoom, mapInstance.getCameraPosition().target.latitude, mapInstance.getCameraPosition().target.longitude);

                    }
                });
                mapInstance.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    public void onMapClick(LatLng point) {
                        Point p = mapInstance.getProjection().toScreenLocation(point);
                        MapContainer.fireTapEventStatic(InternalNativeMapsImpl.this.mapId, p.x, p.y);
                    }
                });
                mapInstance.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

                    public void onMapLongClick(LatLng point) {
                        Point p = mapInstance.getProjection().toScreenLocation(point);
                        MapContainer.fireLongPressEventStatic(InternalNativeMapsImpl.this.mapId, p.x, p.y);
                    }
                });
                mapInstance.setMyLocationEnabled(showMyLocation);
                mapInstance.getUiSettings().setRotateGesturesEnabled(rotateGestureEnabled);
            }
        });
    }

    public void onCreate(Bundle savedInstanceState) {
        try {
            if (view != null) {
                view.onCreate(savedInstanceState);
                initMaps();
                view.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mapInstance = googleMap;
                    }
                });
                //mapInstance = view.getMap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        try {
            if(view != null) {
                //mapInstance = view.getMap();
                view.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mapInstance = googleMap;
                    }
                });
                view.onResume();
                installListeners();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        try {
            if(view != null) {
                view.onPause();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onDestroy() {
        try {
            if(view != null) {
                if (view.getParent() != null) {
                    ((ViewGroup) view.getParent()).removeView(view);
                }
                view.onDestroy();
                initialized = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSaveInstanceState(Bundle b) {
        try {
            if(view != null) {
                view.onSaveInstanceState(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onLowMemory() {
        try {
            if(view != null) {
                view.onLowMemory();
                synchronized(pendingUpdates) {
                    PendingUpdate toRemove = null;
                    for (PendingUpdate u : pendingUpdates) {
                        if (u.view == view) {
                            if (u.timer != null) {
                                u.timer.cancel();
                                u.timer = null;
                            }
                            toRemove = u;
                        }
                    }
                    if (toRemove != null) {
                        pendingUpdates.remove(toRemove);
                    }
                }
                peerImages.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public boolean isSupported() {
        return supported;
    }
    
    
    //////////////
    //////////////
    /////////////
    
    
    public android.view.View createNativeMap(int mapId) {
        this.mapId = mapId;
        //if (showMyLocation) {
        //    if (!AndroidNativeUtil.checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "Show My Location On Map")) {
        //        Log.p("Show my location has been disabled because permission was not granted by the user.");
        //        showMyLocation = false;
        //    }
        //    
        //}
        final boolean[] ready = new boolean[1];
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    view = new MapView(AndroidNativeUtil.getActivity());
                    view.onCreate(AndroidNativeUtil.getActivationBundle());
                    view.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            try {
                                mapInstance = googleMap;
                                installListeners();
                            } finally {
                                synchronized (ready) {
                                    ready[0] = true;
                                    ready.notifyAll();
                                }
                            }
                        }
                    });
                    //mapInstance = view.getMap();


                } catch (Throwable e) {
                    System.out.println("Failed to initialize, google play services not installed: " + e);
                    e.printStackTrace();
                    view = null;
                    synchronized(ready) {
                        ready[0] = true;
                        ready.notifyAll();
                    }
                    return;
                }
                //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
            }
        });
        while (!ready[0]) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    synchronized(ready) {
                        if (!ready[0]) {
                            Util.wait(ready, 30);
                        }
                    }
                }
            });
        }
        return view;
    }

    public void initialize() {
        AndroidNativeUtil.addLifecycleListener(this);        
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    view.invalidate();
                    view.onPause();
                    view.onResume();
                } catch (Exception e) {
                    Log.e(e);
                }
            }
        });
    }
    
    public void deinitialize() {
        AndroidNativeUtil.removeLifecycleListener(this);
        synchronized(pendingUpdates) {
            PendingUpdate toRemove = null;
            for (PendingUpdate u : pendingUpdates) {
                if (u.view == view) {
                    if (u.timer != null) {
                        u.timer.cancel();
                        u.timer = null;
                    }
                    toRemove = u;
                }
            }
            if (toRemove != null) {
                pendingUpdates.remove(toRemove);
            }

        }
        PeerImage.clearOldest();
    }    
    
	
    //
    //Style
    //
    
    public void setMapType(final int param) {
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    switch(param) {
                        case MapContainer.MAP_TYPE_HYBRID:
                            mapInstance.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                            return;
                        case MapContainer.MAP_TYPE_TERRAIN:
                            mapInstance.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                            return;
                    }
                    mapInstance.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
    }
    
    public int getMapType() {
        if(mapInstance != null) { 
            switch(mapInstance.getMapType()) {
                case GoogleMap.MAP_TYPE_HYBRID:
                    return MapContainer.MAP_TYPE_HYBRID;
                case GoogleMap.MAP_TYPE_TERRAIN:
                case GoogleMap.MAP_TYPE_SATELLITE:
                    return MapContainer.MAP_TYPE_TERRAIN;
            }
        }
        return MapContainer.MAP_TYPE_NONE;
    }
    
    public void setPadding (int left, int top, int right, int bottom) {
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                   mapInstance.setPadding(left, top, right, bottom);
                }
            });
        }
    }
    
    public boolean setMapStyle(String json) {
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                  result[0] = mapInstance.setMapStyle(new MapStyleOptions(json));
                }
            });
        }
        return result[0];
    }

    public void setMyLocationEnabled(boolean show) {
        if (show && !showMyLocation) {
            if (!AndroidNativeUtil.checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "Show My Location On Map")) {
                //Log.p("Show my location has been disabled because permission was not granted by the user.");
                System.out.println("Show My Location disabled because user didn't grant ACCESS_FINE_LOCATION permission");
                return;
            }
        }
        showMyLocation = show;
        
        if(mapInstance != null) {
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.setMyLocationEnabled(showMyLocation);
                    //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });            
        }
    }
    
    public boolean isMyLocationEnabled() {
    	final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.isMyLocationEnabled();
                }
            });
        }
        return result[0];
    }
    
    public void	setBuildingsEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                   mapInstance.setBuildingsEnabled(enabled);
                }
            });
        }
    }

    public boolean isBuildingsEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.isBuildingsEnabled();
                }
            });
        }
        return result[0];
    }

    public boolean setIndoorEnabled(boolean enabled){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.setIndoorEnabled(enabled);
                }
            });
        }
        return result[0];
    }
    
    /** Gets whether indoor maps are currently enabled.*/
    public boolean isIndoorEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.isIndoorEnabled();
                }
            });
        }
        return result[0];
    }
    
    public void setTrafficEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                   mapInstance.setTrafficEnabled(enabled);
                }
            });
        }
    }

    public boolean isTrafficEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.isTrafficEnabled();
                }
            });
        }
        return result[0];
    }
      
    
    //
    //UiSettings methods
    //
    
    public boolean isCompassEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isCompassEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isIndoorLevelPickerEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isIndoorLevelPickerEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isMapToolbarEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isMapToolbarEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isMyLocationButtonEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isMyLocationButtonEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isRotateGesturesEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isRotateGesturesEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isScrollGesturesEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isScrollGesturesEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isTiltGesturesEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isTiltGesturesEnabled();
                }
            });
        }
        return result[0];
    }
    
    public boolean isZoomControlsEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isZoomControlsEnabled();
                }
            });
        }
        return result[0];
    }

    public boolean isZoomGesturesEnabled(){
        final boolean[] result = new boolean[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getUiSettings().isZoomGesturesEnabled();
                }
            });
        }
        return result[0];
    }
    
    public void	setAllGesturesEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setAllGesturesEnabled(enabled);
                }
            });
        }
    }

    public void setCompassEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setCompassEnabled(enabled);
                }
            });
        }
    }

    public void	setIndoorLevelPickerEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setIndoorLevelPickerEnabled(enabled);
                }
            });
        }
    }

    public void	setMapToolbarEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setMapToolbarEnabled(enabled);
                }
            });
        }
    }

    public void	setMyLocationButtonEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setMyLocationButtonEnabled(enabled);
                }
            });
        }
    }

    public void setRotateGesturesEnabled(boolean enabled){
        rotateGestureEnabled = enabled;
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setRotateGesturesEnabled(rotateGestureEnabled);
                }
            });
        }
    }
    
    public void	setScrollGesturesEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setScrollGesturesEnabled(enabled);
                }
            });
        }
    }

    public void	setTiltGesturesEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setTiltGesturesEnabled(enabled);
                }
            });
        }
    }
    
    public void	setZoomControlsEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setZoomControlsEnabled(enabled);
                }
            });
        }
    }

    public void	setZoomGesturesEnabled(boolean enabled){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.getUiSettings().setZoomGesturesEnabled(enabled);
                }
            });
        }
    }
    
    
    //
    //Camera
    //
    
    public void setPosition(final double lat, final double lon) {
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
                    PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
    }
    
    public void animatePosition(double lat, double lon, int durationMs){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)), durationMs, new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }

                        @Override
                        public void onCancel() {
                           PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }
                    }
                    );
                    
                }
            });
        }
    }
    
    public double getLatitude() {
        final double[] result = new double[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getCameraPosition().target.latitude;
                }
            });
        }
        return result[0];
    }
    
    public double getLongitude() {
        final double[] result = new double[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getCameraPosition().target.longitude;
                }
            });
        }
        return result[0];
    }
    
    public void setZoom(float zoom){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.moveCamera(CameraUpdateFactory.zoomTo(zoom));
                    PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
    }
   
    public void animateZoom(float zoom, int durationMs){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.animateCamera(CameraUpdateFactory.zoomTo(zoom), durationMs, new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }

                        @Override
                        public void onCancel() {
                           PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }
                    }
                    );
                    
                }
            });
        }
    }
    
    public float getZoom() {
        final float[] result = new float[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getCameraPosition().zoom;

                }
            });
        }
        return result[0];
    }

    public void setCamera(final double lat, final double lon, final float zoom) {
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom));
                    PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
    }
    
    public void animateCamera(double lat, double lon, float zoom, int durationMs){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mapInstance.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom), durationMs, new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }

                        @Override
                        public void onCancel() {
                           PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                        }
                    }
                    );
                    
                }
            });
        }
    }
    
    public void setMaxZoom(float maxZoom){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.setMaxZoomPreference(maxZoom);
                }
            });
        }
    }
    
    public void setMinZoom(float minZoom){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.setMinZoomPreference(minZoom);
                }
            });
        }
    }
      
    public void resetMinMaxZoomPreference(){
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapInstance.resetMinMaxZoomPreference();
                }
            });
        }
    }

    public float getMaxZoom() {
        final float[] result = new float[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getMaxZoomLevel();
                }
            });
        }
        return result[0];
    }

    public float getMinZoom() {
        final float[] result = new float[1];
        if(mapInstance != null) { 
            AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    result[0] = mapInstance.getMinZoomLevel();
                }
            });
        }
        return result[0];
    }
    
    
    //
    //Map elements
    //
    
    public void setMarkerSize(int width, int height) {
        // not needed right now... used only by Javascript port
    }
    
    public long addMarker(final byte[] icon, final double lat, final double lon, final String text, final String snippet, final boolean callback, final float anchorU, final float anchorV) {
        uniqueIdCounter++;
        final long key = uniqueIdCounter;
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    MarkerOptions mo = new MarkerOptions();
                    mo.anchor(anchorU, anchorV);
                    if(text != null) {
                        mo.title(text);
                    }
                    if(icon != null) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0, icon.length);
                        mo.icon(BitmapDescriptorFactory.fromBitmap(bmp));
                    }
                    if(snippet != null) {
                        mo.snippet(snippet);
                    }
                    mo.position(new LatLng(lat, lon));

                    Marker m = mapInstance.addMarker(mo);
                    if(callback) {
                        listeners.put(m, key);
                    }
                    markerLookup.put(key, m);
                    //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
        return key;
    }
    
    public long beginPath() {
        currentPath = new PolylineOptions();
        return 1;
    }
    
    public void addToPath(long param, double param1, double param2) {
        currentPath.add(new LatLng(param1, param2));
    }
    
    public long finishPath(long param) {
        uniqueIdCounter++;
        final long key = uniqueIdCounter;
        if(mapInstance != null) { 
            AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    paths.put(key, mapInstance.addPolyline(currentPath));
                    //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
                }
            });
        }
        return key;
    }
    
    public void removeMapElement(final long param) {
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Marker m = markerLookup.get(param);
                if(m != null) {
                    m.remove();
                    markerLookup.remove(param);
                    listeners.remove(m);
                    return;
                }
                
                Polyline p = paths.get(param);
                if(p != null) {
                    p.remove();
                    paths.remove(param);
                }
                //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());
            }
        });
    }
    
    public void removeAllMarkers() {
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(mapInstance != null) { 
                    mapInstance.clear();
                }
                markerLookup.clear();
                listeners.clear();
                //PeerImage.submitUpdate(view, view.getWidth(), view.getHeight());

            }
        });
    }
      
    
    
    
    //
    //screen/geopgraphy conversion
    //
    
    public void calcScreenPosition(final double lat, final double lon) {
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                lastPoint = mapInstance.getProjection().toScreenLocation(new LatLng(lat, lon));
            }
        });
    }
    
    public int getScreenX() {
        return lastPoint.x;
    }
    
    public int getScreenY() {
        return lastPoint.y;
    }

    public void calcLatLongPosition(final int x, final int y) {
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                lastPosition = mapInstance.getProjection().fromScreenLocation(new Point(x, y));
            }
        });
    }
    
    public double getScreenLat() {
        return lastPosition.latitude;
    }
    
    public double getScreenLon() {
        return lastPosition.longitude;
    }
    
    
    
    
}
