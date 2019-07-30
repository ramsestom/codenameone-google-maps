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
#import <Foundation/Foundation.h>
#import "GoogleMaps/GoogleMaps.h"

@interface com_codename1_googlemaps_InternalNativeMapsImpl : NSObject<GMSMapViewDelegate> {
    GMSMapView *mapView;
    int mapId;
    UIColor pathColor;
    int pathThickness;
    BOOL pathGeodesic;
    CGPoint currentPoint;
    CLLocationCoordinate2D currentCoordinate;
    BOOL showMyLocation;
    BOOL rotateGesture;
    BOOL scrollGesture;
    BOOL tiltGesture;
    BOOL zoomGesture;
}

-(BOOL)isSupported;
-(void*)createNativeMap:(int)param;
-(void)initialize;
-(void)deinitialize;

-(void)setMapType:(int)param;
-(int)getMapType;
-(void)setPadding:(int)param param1:(int)param1 param2:(int)param2 param3:(int)param3;
-(BOOL)setMapStyle:(NSString*)param;
-(void)setMyLocationEnabled:(BOOL)param;
-(BOOL)isMyLocationEnabled;
-(void)setBuildingsEnabled:(BOOL)param;
-(BOOL)isBuildingsEnabled;
-(BOOL)setIndoorEnabled:(BOOL)param;
-(BOOL)isIndoorEnabled;
-(void)setTrafficEnabled:(BOOL)param;
-(BOOL)isTrafficEnabled;

-(void)setCompassEnabled:(BOOL)param;
-(BOOL)isCompassEnabled;
-(void)setIndoorLevelPickerEnabled:(BOOL)param;
-(BOOL)isIndoorLevelPickerEnabled;
-(void)setMapToolbarEnabled:(BOOL)param;
-(BOOL)isMapToolbarEnabled;
-(void)setMyLocationButtonEnabled:(BOOL)param;
-(BOOL)isMyLocationButtonEnabled;
-(BOOL)isZoomGesturesEnabled;
-(void)setZoomControlsEnabled:(BOOL)param;
-(void)disableDefaultUI;

-(void)setRotateGesturesEnabled:(BOOL)param;
-(BOOL)isRotateGesturesEnabled;
-(void)setScrollGesturesEnabled:(BOOL)param;
-(BOOL)isScrollGesturesEnabled;
-(void)setTiltGesturesEnabled:(BOOL)param;
-(BOOL)isTiltGesturesEnabled;
-(void)setZoomGesturesEnabled:(BOOL)param;
-(BOOL)isZoomControlsEnabled;
-(void)setAllGesturesEnabled:(BOOL)param;

-(void)setPosition:(double)param param1:(double)param1;
-(void)animatePosition:(double)param param1:(double)param1 param2:(int)param2;
-(double)getLatitude;
-(double)getLongitude;
-(void)setZoom:(float)param;
-(void)animateZoom:(float)param param1:(int)param1;
-(float)getZoom;
-(void)setCamera:(double)param param1:(double)param1 param2:(float)param2;
-(void)animateCamera:(double)param param1:(double)param1 param2:(float)param2 param3:(int)param3;
-(void)setMaxZoom:(float)param;
-(void)setMinZoom:(float)param;
-(void)resetMinMaxZoomPreference;
-(float)getMaxZoom;
-(float)getMinZoom;
-(void)setBearing:(float)param;
-(float)getBearing;
-(void)setTilt:(float)param;
-(float)getTilt;
-(void)stopAnimation;

-(void)setMarkerSize:(int)param param1:(int)param1;
-(long long)addMarker:(NSData*)param param1:(double)param1 param2:(double)param2 param3:(NSString*)param3 param4:(NSString*)param4 param5:(BOOL)param5 param6:(float)param6 param7:(float)param7;
-(void)setPathColor:(int)param;
-(void)restorePathDefaultColor;
-(void)setPathThickness:(int)param;
-(void)restorePathDefaultThickness;
-(void)setPathGeodesic:(BOOL)param;
-(void)restorePathDefaultGeodesic;
-(long long)beginPath;
-(void)addToPath:(long long)param param1:(double)param1 param2:(double)param2;
-(long long)finishPath:(long long)param;
-(void)removeMapElement:(long long)param;
-(void)removeAllMarkers;

-(void)calcScreenPosition:(double)param param1:(double)param1;
-(int)getScreenX;
-(int)getScreenY;
-(void)calcLatLongPosition:(int)param param1:(int)param1;
-(double)getScreenLat;
-(double)getScreenLon;

@end
