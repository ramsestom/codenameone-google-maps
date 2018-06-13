using com.codename1.impl;
using Windows.Devices.Geolocation;
using Windows.UI.Xaml.Controls.Maps;
using System;
namespace com.codename1.googlemaps{
   

public class InternalNativeMapsImpl : IInternalNativeMapsImpl {

    const string TOKEN_KEY = "windows.bingmaps.token";

    MapControl mapControl;
    int mapId;
    int currPx;
    int currPy;
    double currLat;
    double currLng;

    public bool isSupported() {
        return com.codename1.ui.Display.getInstance().getProperty(TOKEN_KEY, null) != null ;
    }

    public object createNativeMap(int param) {
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
                mapControl = new MapControl();
                mapId = param;
                mapControl.ZoomInteractionMode = MapInteractionMode.GestureAndControl;
                mapControl.TiltInteractionMode = MapInteractionMode.GestureAndControl;
                string token = com.codename1.ui.Display.getInstance().getProperty(TOKEN_KEY, "");
            
                mapControl.MapServiceToken = token;
                mapControl.ActualCameraChanged += MapControl_ActualCameraChanged;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();

            return mapControl;
    }

    public void initialize() {
    }

    public void deinitialize() {
    }



    public void setMapType(int param) {
    }

    public int getMapType() {
        return 0;
    }

    public void setPadding(int param, int param1, int param2, int param3) {
    }

    public bool setMapStyle(String param) {
        return false;
    }

    public void setMyLocationEnabled(bool param) {
    }

    public bool isMyLocationEnabled() {
        return false;
    }

    public void setBuildingsEnabled(bool param) {
    }

    public bool isBuildingsEnabled() {
        return false;
    }

    public bool setIndoorEnabled(bool param) {
        return false;
    }

    public bool isIndoorEnabled() {
        return false;
    }

    public void setTrafficEnabled(bool param) {
    }

    public bool isTrafficEnabled() {
        return false;
    }



    public void setCompassEnabled(bool param) {
    }

    public bool isCompassEnabled() {
        return false;
    }

    public void setIndoorLevelPickerEnabled(bool param) {
    }
    
    public bool isIndoorLevelPickerEnabled() {
        return false;
    }

    public void setMapToolbarEnabled(bool param) {
    }

    public bool isMapToolbarEnabled() {
        return false;
    }

    public void setMyLocationButtonEnabled(bool param) {
    }

    public bool isMyLocationButtonEnabled() {
        return false;
    }
    
    public void setZoomControlsEnabled(bool param) {
    }

    public bool isZoomControlsEnabled() {
        return false;
    }


    public void setRotateGesturesEnabled(bool param) {
    }

    public bool isRotateGesturesEnabled() {
        return false;
    }

    public void setScrollGesturesEnabled(bool param) {
    }

    public bool isScrollGesturesEnabled() {
        return false;
    }

    public void setTiltGesturesEnabled(bool param) {
    }

    public bool isTiltGesturesEnabled() {
        return false;
    }

    public void setZoomGesturesEnabled(bool param) {
    }

    public bool isZoomGesturesEnabled() {
        return false;
    }

    public void setAllGesturesEnabled(bool param) {
    }



    public void setPosition(double param, double param1) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = param, Longitude = param1 };
            Geopoint cityCenter = new Geopoint(cityPosition);
            mapControl.Center = cityCenter;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void animatePosition(double param, double param1, int param2) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = param, Longitude = param1 };
            Geopoint cityCenter = new Geopoint(cityPosition);
            mapControl.Center = cityCenter;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public double getLatitude() {
            double res = 0;
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
               res = mapControl.Center.Position.Latitude;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            return res;
    }

    public double getLongitude() {
            double res = 0;
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
                res = mapControl.Center.Position.Longitude;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            return res;
    }

    public void setZoom(float param) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            mapControl.ZoomLevel = param;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void animateZoom(float param, int param1) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            mapControl.ZoomLevel = param;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public float getZoom() {
        float res = 0;
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            res = (float)mapControl.ZoomLevel;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        return res;
    }

    public void setCamera(double param, double param1, float param2) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = param, Longitude = param1 };
            Geopoint cityCenter = new Geopoint(cityPosition);
            mapControl.Center = cityCenter;
            mapControl.ZoomLevel = param2;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void animateCamera(double param, double param1, float param2, int param3) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = param, Longitude = param1 };
            Geopoint cityCenter = new Geopoint(cityPosition);
            mapControl.Center = cityCenter;
            mapControl.ZoomLevel = param2;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void setMaxZoom(float param) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            mapControl.MaxZoomLevel = param;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void setMinZoom(float param) {
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            mapControl.MinZoomLevel = param;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public void resetMinMaxZoomPreference() {
    }

    public float getMaxZoom() {
        int res = 0;
        SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
        {
            res = (float)mapControl.MaxZoomLevel;
        }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        return res;
    }

    public float getMinZoom() {
            int res = 0;
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
               res = (float)mapControl.MinZoomLevel;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            return res;
    }




    public void setMarkerSize(int w, int h) {
    }

    public long addMarker(byte[] param, double param1, double param2, string param3, string param4, bool param5, float param6, float param7) {
        return 0;
    }

    public long beginPath() {
        return 0;
    }

    public void addToPath(long param, double param1, double param2) {
    }

    public long finishPath(long param) {
        return 0;
    }

    public void removeMapElement(long param) {
    }

    public void removeAllMarkers() {
    }



    public void calcScreenPosition(double param, double param1) {
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
                Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = param, Longitude = param1 };
                Geopoint pt = new Geopoint(cityPosition);
                Windows.Foundation.Point p = new Windows.Foundation.Point();
                mapControl.GetOffsetFromLocation(pt, out p);
                currPx = (int)(p.X*SilverlightImplementation.scaleFactor);
                currPy = (int)(p.Y*SilverlightImplementation.scaleFactor);
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public int getScreenX() {
            return currPx;
    }
   
    public int getScreenY() {
            return currPy;
    }

    public void calcLatLongPosition(int param, int param1) {
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
                Windows.Devices.Geolocation.BasicGeoposition cityPosition = new BasicGeoposition() { Latitude = 0, Longitude = 0 };
                Geopoint pt = new Geopoint(cityPosition);
                Windows.Foundation.Point p = new Windows.Foundation.Point(param/SilverlightImplementation.scaleFactor, param1/SilverlightImplementation.scaleFactor);
                mapControl.GetLocationFromOffset(p, out pt);
                currLat = pt.Position.Latitude;
                currLng = pt.Position.Longitude;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
    }

    public double getScreenLat() {
        return currLat;
    }

    public double getScreenLon() {
            return currLng;
    }


    private void MapControl_ActualCameraChanged(MapControl sender, MapActualCameraChangedEventArgs args){
        com.codename1.googlemaps.MapContainer.fireMapChangeEvent(mapId, (int)mapControl.ZoomLevel, mapControl.Center.Position.Latitude, mapControl.Center.Position.Longitude);
    }

  }
}
