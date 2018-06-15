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

import com.codename1.system.NativeInterface;
import com.codename1.ui.PeerComponent;

/**
 * This is an internal implementation class
 *
 * @author Shai Almog
 * @deprecated used internally please use MapContainer
 */
public interface InternalNativeMaps extends NativeInterface {

    public PeerComponent createNativeMap(int mapId);
	
    public void initialize();
    
    public void deinitialize();
    
    
    //Style
    
    public void setMapType(int type);
    
    public int getMapType();
    
    /** Sets padding on the map.*/
    public void setPadding(int left, int top, int right, int bottom);
    
    /** Sets the styling of the base map.*/
    public boolean setMapStyle(String json);

    /** Enables or disables the my-location layer*/
    public void	setMyLocationEnabled(boolean enabled); //public void setShowMyLocation(boolean show);
    
    /** Gets the status of the my-location layer.*/
    public boolean isMyLocationEnabled();
    
    /** Turns the 3D buildings layer on or off.*/
    public void	setBuildingsEnabled(boolean enabled);

    /** Returns whether 3D buildings layer is enabled.*/
    public boolean isBuildingsEnabled();

    /** Sets whether indoor maps should be enabled.*/
    public boolean setIndoorEnabled(boolean enabled);
    
    /** Gets whether indoor maps are currently enabled.*/
    public boolean isIndoorEnabled();
    
    /** Turns the traffic layer on or off.*/
    public void setTrafficEnabled(boolean enabled);

    /** Checks whether the map is drawing traffic data.*/
    public boolean isTrafficEnabled();
    
    
    
    //UiSettings methods
    
    /** Enables or disables the compass.*/
    public void setCompassEnabled(boolean enabled);
    
    /** Gets whether the compass is enabled/disabled.*/
    public boolean isCompassEnabled();
    
    /** Sets whether the indoor level picker is enabled when indoor mode is enabled.*/
    public void	setIndoorLevelPickerEnabled(boolean enabled);
    
    /** Gets whether the indoor level picker is enabled/disabled.*/
    public boolean isIndoorLevelPickerEnabled();
    
    /** Sets the preference for whether the Map Toolbar should be enabled or disabled.*/
    public void	setMapToolbarEnabled(boolean enabled);
    
    /** Gets whether the Map Toolbar is enabled/disabled. */
    public boolean isMapToolbarEnabled();
    
     /** Enables or disables the my-location button.*/
    public void	setMyLocationButtonEnabled(boolean enabled);
    
    /** Gets whether the my-location button is enabled/disabled.*/
    public boolean isMyLocationButtonEnabled();
    
     /** Enables or disables the zoom controls.*/
    public void	setZoomControlsEnabled(boolean enabled);
    
    /** Gets whether the zoom controls are enabled/disabled.*/
    public boolean isZoomControlsEnabled();
    
    /** Disable all default UI buttons */
    public void disableDefaultUI();
    
    
    /** Sets the preference for whether rotate gestures should be enabled or disabled.*/
    public void setRotateGesturesEnabled(boolean enabled); //public void setRotateGestureEnabled(boolean e);
    
    /** Gets whether rotate gestures are enabled/disabled.*/
    public boolean isRotateGesturesEnabled();
    
    /** Sets the preference for whether scroll gestures should be enabled or disabled.*/
    public void	setScrollGesturesEnabled(boolean enabled);
    
    /** Gets whether scroll gestures are enabled/disabled.*/
    public boolean isScrollGesturesEnabled();
    
    /** Sets the preference for whether tilt gestures should be enabled or disabled.*/
    public void	setTiltGesturesEnabled(boolean enabled);
    
    /** Gets whether tilt gestures are enabled/disabled.*/
    public boolean isTiltGesturesEnabled();
    
    /** Sets the preference for whether zoom gestures should be enabled or disabled.*/
    public void	setZoomGesturesEnabled(boolean enabled);
    
    /** Gets whether zoom gestures are enabled/disabled.*/
    public boolean isZoomGesturesEnabled();
    
    /** Sets the preference for whether all gestures should be enabled or disabled.*/
    public void	setAllGesturesEnabled(boolean enabled);
    
    
    //Camera
    
    public void setPosition(double lat, double lon);
    
    public void animatePosition(double lat, double lon, int durationMs);
    
    public double getLatitude();
    
    public double getLongitude();
    
    /** imediately change the map to this zoom level*/
    public void setZoom(float zoom);
   
    public void animateZoom(float zoom, int durationMs);
    
    public float getZoom();
    
    public void setCamera(double lat, double lon, float zoom);
    
    public void animateCamera(double lat, double lon, float zoom, int durationMs);
    
    /** Sets a preferred upper bound for the camera zoom. */
    public void setMaxZoom(float maxZoom);
    
    /** Sets a preferred lower bound for the camera zoom.*/
    public void setMinZoom(float minZoom);
      
    /** Removes any previously specified upper and lower zoom bounds. */
    public void resetMinMaxZoomPreference();

    public float getMaxZoom();

    public float getMinZoom();
    
    /** Set the direction that the camera is pointing in, in degrees clockwise from north. */
    public void setBearing(float angle);

    public float getBearing();
    
    /** Set the angle, in degrees, of the camera angle from the nadir (directly facing the Earth). */
    public void setTilt(float angle);

    public float getTilt();
    
    /** Stops the camera animation if there is one in progress. */
    public void stopAnimation();
    

    
    //Map elements
    
    public void setMarkerSize(int width, int height);
    
    public long addMarker(byte[] icon, double lat, double lon, String text, String longText, boolean callback, float anchorU, float anchorV);
    
    public long beginPath();
    
    public void addToPath(long pathId, double lat, double lon);
    
    public long finishPath(long pathId);
    
    public void removeMapElement(long id);
    
    public void removeAllMarkers();
 
        
    //screen/geopgraphy conversion
    
    public void calcScreenPosition(double lat, double lon);
    
    public int getScreenX();
    
    public int getScreenY();

    public void calcLatLongPosition(int x, int y);
    
    public double getScreenLat();
    
    public double getScreenLon();
}
