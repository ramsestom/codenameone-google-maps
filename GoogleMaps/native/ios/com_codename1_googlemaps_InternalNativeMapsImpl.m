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

#import "com_codename1_googlemaps_InternalNativeMapsImpl.h"
#include "com_codename1_googlemaps_MapContainer.h"
#import "CodenameOne_GLViewController.h"

extern float scaleValue;

@implementation com_codename1_googlemaps_InternalNativeMapsImpl

-(BOOL)isSupported{
    return YES;
}

-(void*)createNativeMap:(int)param{
    mapId = param;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:-33.86
                                                                longitude:151.20
                                                                     zoom:6];
        mapView = [GMSMapView mapWithFrame:CGRectZero camera:camera];
        mapView.myLocationEnabled = showMyLocation;
        mapView.settings.compassButton = showMyLocation;
        mapView.settings.myLocationButton = showMyLocation;
        mapView.settings.rotateGestures = rotateGesture;
        mapView.delegate = self;
        [mapView retain];
        [pool release];
    });
    return mapView;
}

-(void)deinitialize {}

-(void)initialize {}

-(void)setMapType:(int)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        switch(param) {
            case 1:
                mapView.mapType = kGMSTypeSatellite;
                return;
            case 2:
                mapView.mapType = kGMSTypeHybrid;
                return;
        }
        mapView.mapType = kGMSTypeNormal;
    });
}

-(int)getMapType{
    GMSMapViewType t = mapView.mapType;
    if(t == kGMSTypeSatellite) {
        return 1;
    }
    if(t == kGMSTypeHybrid) {
        return 2;
    }
    if(t == kGMSTypeTerrain) {
        return 1;
    }
    return 3;
}

-(void)setPadding:(int)param param1:(int)param1 param2:(int)param2 param3:(int)param3{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            UIEdgeInsets mapInsets = UIEdgeInsetsMake(param1, param, param3, param2); 
            mapView.padding = mapInsets;
        }
    });
}

-(BOOL)setMapStyle:(NSString*)param{
    __block BOOL allok = NO;
    if(mapView != nil) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            GMSMapStyle *style = [GMSMapStyle styleWithJSONString:param error:&error];
            if (!style) {
                NSLog(@"The style definition could not be loaded: %@", error);
                allok = NO; 
            } else {
                mapView.mapStyle = style;
                allok = YES;
            }
        });
    }
    return allok;
}

-(void)setMyLocationEnabled:(BOOL)param{
    showMyLocation = param;
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.myLocationEnabled = showMyLocation;
        } 
    });
}

-(BOOL)isMyLocationEnabled{
    __block BOOL mle = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mle = mapView.myLocationEnabled;
        }
    }
    return mle;
}

-(void)setBuildingsEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.buildingsEnabled = param;
        } 
    });
}

-(BOOL)isBuildingsEnabled{
    __block BOOL be = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            be = mapView.buildingsEnabled;
        }
    }
    return be;
}

-(BOOL)setIndoorEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.indoorEnabled = param;
        } 
    });
}

-(BOOL)isIndoorEnabled{
    __block BOOL ine = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            ine = mapView.indoorEnabled;
        }
    }
    return ine;
}

-(void)setTrafficEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.trafficEnabled = param;
        } 
    });
}

-(BOOL)isTrafficEnabled{
    __block BOOL trafe = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            trafe = mapView.trafficEnabled;
        }
    }
    return trafe;
}



-(void)setCompassEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.compassButton = param;
        } 
    });
}

-(BOOL)isCompassEnabled{
    __block BOOL compe = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            compe = mapView.settings.compassButton;
        }
    }
    return compe;
}

-(void)setIndoorLevelPickerEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.indoorPicker = param;
        } 
    });
}

-(BOOL)isIndoorLevelPickerEnabled{
    __block BOOL inbe = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            inbe = mapView.settings.indoorPicker;
        }
    }
    return inbe;
}

-(void)setMapToolbarEnabled:(BOOL)param{
    //TODO: return an error telling that it is not supported by the iOS port for now
}

-(BOOL)isMapToolbarEnabled{
    return NO;
}

-(void)setMyLocationButtonEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.myLocationButton = param;
        } 
    });
}

-(BOOL)isMyLocationButtonEnabled{
    __block BOOL mlbe = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mlbe = mapView.settings.myLocationButton;
        }
    }
    return mlbe;
}

-(void)setZoomControlsEnabled:(BOOL)param{
    //TODO: return an error telling that it is not supported by the iOS port for now
}

-(BOOL)isZoomControlsEnabled{
    return NO;
}


-(void)setRotateGesturesEnabled:(BOOL)param{
    rotateGesture = param;
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.rotateGestures = rotateGesture;
        } 
    });
}

-(BOOL)isRotateGesturesEnabled{
    __block BOOL rge = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            rge = mapView.settings.rotateGestures;
        }
    }
    return rge;
}

-(void)setScrollGesturesEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.scrollGestures = param;
        } 
    });
}

-(BOOL)isScrollGesturesEnabled{
    __block BOOL sge = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            sge = mapView.settings.scrollGestures;
        }
    }
    return sge;
}

-(void)setTiltGesturesEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.tiltGestures = param;
        } 
    });
}

-(BOOL)isTiltGesturesEnabled{
    __block BOOL tge = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            tge = mapView.settings.tiltGestures;
        }
    }
    return tge;
}

-(void)setZoomGesturesEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            mapView.settings.zoomGestures = param;
        } 
    });
}

-(BOOL)isZoomGesturesEnabled{
    __block BOOL zge = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            zge = mapView.settings.zoomGestures;
        }
    }
    return zge;
}

-(void)setAllGesturesEnabled:(BOOL)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            [mapView.settings setAllGesturesEnabled:param];
        } 
    });
}




-(void)setPosition:(double)param param1:(double)param1{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:param
                                                             longitude:param1
                                                             zoom:mapView.camera.zoom
                                                             bearing:mapView.camera.bearing	
                                                             viewingAngle:mapView.camera.viewingAngle];
            mapView.camera = camera;
            [mapView retain];
            [pool release];
        }
    });
}

-(void)animatePosition:(double)param param1:(double)param1 param2:(int)param2{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            [mapView animateToLocation:CLLocationCoordinate2DMake(param, param1)];
            [mapView retain];
            [pool release];
        }
    });
}

-(double)getLatitude{
    __block double lat = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            lat = mapView.camera.target.latitude;
        }
    });
    return lat;
}

-(double)getLongitude{
    __block double lon = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            lon = mapView.camera.target.longitude;
        }
    });
    return lon;
}

-(void)setZoom:(float)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            GMSCameraPosition *camera = [GMSCameraPosition cameraWithTarget:mapView.camera.target
                                                           zoom:param
                                                           bearing:mapView.camera.bearing	
                                                           viewingAngle:mapView.camera.viewingAngle];
            mapView.camera = camera;
            [mapView retain];
            [pool release];
        }
    });
}

-(void)animateZoom:(float)param param1:(int)param1{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            [mapView animateToZoom:param];
            [mapView retain];
            [pool release];
        }
    });
}

-(float)getZoom{
    __block float zoom = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            zoom = mapView.camera.zoom;
        }
    });
    return zoom;
}

-(void)setCamera:(double)param param1:(double)param1 param2:(float)param2{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:param
                                                           longitude:param1
                                                           zoom:param2
                                                           bearing:mapView.camera.bearing	
                                                           viewingAngle:mapView.camera.viewingAngle];
            mapView.camera = camera;
            [mapView retain];
            [pool release];
        }
    });
}

-(void)animateCamera:(double)param param1:(double)param1 param2:(float)param2 param3:(int)param3{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
            GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:param
                                                           longitude:param1
                                                           zoom:param2
                                                           bearing:mapView.camera.bearing	
                                                           viewingAngle:mapView.camera.viewingAngle];
            [mapView  animateToCameraPosition:camera];
            [mapView retain];
            [pool release];
        }
    });
}

-(void)setMaxZoom:(float)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            [mapView setMinZoom:mapView.minZoom
                     maxZoom:param];
        }
    });
}

-(void)setMinZoom:(float)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            [mapView setMinZoom:param
                     maxZoom:mapView.maxZoom];
        }
    });
}

-(void)resetMinMaxZoomPreference{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            [mapView setMinZoom:kGMSMinZoomLevel
                     maxZoom:kGMSMaxZoomLevel];
        }
    });
}

-(float)getMaxZoom{
    __block float zoom = kGMSMaxZoomLevel;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            zoom = mapView.maxZoom;
        }
    });
    return zoom;
}

-(float)getMinZoom{
    __block float zoom = kGMSMinZoomLevel;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(mapView != nil) {
            zoom = mapView.minZoom;
        }
    });
    return zoom;
}




-(void)setMarkerSize:(int)param param1:(int)param1 {
    // Not needed right now.
    // Only used by Javascript port
}

-(long long)addMarker:(NSData*)param param1:(double)param1 param2:(double)param2 param3:(NSString*)param3 param4:(NSString*)param4 param5:(BOOL)param5 param6:(float)param6 param7:(float)param7{
    __block GMSMarker *marker = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        marker = [[GMSMarker alloc] init];
        marker.position = CLLocationCoordinate2DMake(param1, param2);
        marker.title = param3;
        marker.snippet = param4;
        marker.groundAnchor = CGPointMake(param6, param7);
        if(param != nil) {
            UIImage* img = nil;
            if ([[UIImage class] respondsToSelector:@selector(imageWithData:scale:)]){
                // If we are on retina we need to provide scale, or the images will be too big and 
                // blurry.
                // Scale version available only in iOS 6 and later so check here.
                img = [UIImage imageWithData:param scale:scaleValue];
            } else {
                img = [UIImage imageWithData:param];
            }
            marker.icon = img;
        }
        marker.map = mapView;
        marker.tappable = YES;
        if(param5) {
            marker.userData = @"";
        } else {
            marker.userData = nil;
        }
        
        [marker retain];
        [pool release];
    });
    
    return marker;
}

-(long long)beginPath{
    GMSMutablePath *path = [GMSMutablePath path];
    return path;
}

-(void)addToPath:(long long)param param1:(double)param1 param2:(double)param2{
    GMSMutablePath *path = (GMSMutablePath*) param;
    [path addLatitude:param1 longitude:param2];
}

-(long long)finishPath:(long long)param{
    __block GMSPolyline *polyline = nil;
    dispatch_async(dispatch_get_main_queue(), ^{
        GMSMutablePath *path = (GMSMutablePath*)param;
        polyline = [GMSPolyline polylineWithPath:path];
        polyline.map = mapView;
    });
    return polyline;
}

-(void)removeMapElement:(long long)param{
    NSObject* n = (NSObject*)param;
    if([n isKindOfClass:[GMSMarker class]]) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            GMSMarker* marker = (GMSMarker*)n;
            marker.map = nil;
        });
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        GMSPolyline *polyline = (GMSPolyline *)param;
        polyline.map = nil;
    });
}

-(void)removeAllMarkers{
    dispatch_async(dispatch_get_main_queue(), ^{
        [mapView clear];
    });
}



-(void)calcScreenPosition:(double)param  param1:(double)param1 {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        currentPoint = [mapView.projection pointForCoordinate:CLLocationCoordinate2DMake(param, param1)];
        [pool release];
    });
}
    
-(int)getScreenX {
    return currentPoint.x * scaleValue;
}
    
-(int) getScreenY {
    return currentPoint.y * scaleValue;
}

-(void) calcLatLongPosition:(int)param param1:(int)param1 {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        currentCoordinate = [mapView.projection coordinateForPoint:CGPointMake(param / scaleValue, param1 / scaleValue)];
        [pool release];
    });
}
    
-(double) getScreenLat {
    return currentCoordinate.latitude;
}
    
-(double) getScreenLon {
    return currentCoordinate.longitude;
}



- (void) mapView:(GMSMapView *)mapView didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {
    CGPoint pp = [mapView.projection pointForCoordinate:coordinate];
    com_codename1_googlemaps_MapContainer_fireTapEventStatic___int_int_int(CN1_THREAD_GET_STATE_PASS_ARG mapId, pp.x * scaleValue, pp.y * scaleValue);
}

- (void) mapView:(GMSMapView *)mapView didLongPressAtCoordinate:(CLLocationCoordinate2D)coordinate {
    CGPoint pp = [mapView.projection pointForCoordinate:coordinate];
    com_codename1_googlemaps_MapContainer_fireLongPressEventStatic___int_int_int(CN1_THREAD_GET_STATE_PASS_ARG mapId, pp.x * scaleValue, pp.y * scaleValue);
}
-(void)mapView:(GMSMapView *)mapView didChangeCameraPosition:(GMSCameraPosition *)position {
    com_codename1_googlemaps_MapContainer_fireMapChangeEvent___int_int_double_double(CN1_THREAD_GET_STATE_PASS_ARG mapId, (int)mapView.camera.zoom, mapView.camera.target.latitude, mapView.camera.target.longitude);
}

-(BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {
    if(marker.userData != nil) {
        com_codename1_googlemaps_MapContainer_fireMarkerEvent___int_long(CN1_THREAD_GET_STATE_PASS_ARG mapId, marker);
        return YES;
    }
    return NO;
}

@end
