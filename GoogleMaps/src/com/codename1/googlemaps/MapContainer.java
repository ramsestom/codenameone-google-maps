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

import com.codename1.components.WebBrowser;
import com.codename1.io.Log;
import com.codename1.io.Util;
//import com.codename1.javascript.JSFunction;
//import com.codename1.javascript.JSObject;
import com.codename1.javascript.JavascriptContext;
import com.codename1.location.LatLng;
import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.maps.MapComponent;
import com.codename1.maps.MapListener;
import com.codename1.maps.Mercator;
import com.codename1.maps.Tile;
import com.codename1.maps.layers.LinesLayer;
import com.codename1.maps.layers.PointLayer;
import com.codename1.maps.layers.PointsLayer;
import com.codename1.ui.Container;
import com.codename1.maps.providers.GoogleMapsProvider;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.providers.OpenStreetMapProvider;
import com.codename1.system.NativeLookup;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.MathUtil;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract Map API that encapsulates the device native map and seamlessly replaces
 * it with MapComponent when unsupported by the platform.
 *
 * @author Shai Almog
 */
@SuppressWarnings("deprecation")
public class MapContainer extends Container {
    private boolean debug;
  
    public static final int MAP_TYPE_NORMAL = 0; // = ROADMAP type
    
    public static final int MAP_TYPE_SATELLITE = 1;
    
    public static final int MAP_TYPE_HYBRID = 2;

    public static final int MAP_TYPE_TERRAIN = 3;
    
    public static final int MAP_TYPE_NONE = 4;
    
    
    private InternalNativeMaps internalNative;
    private MapComponent internalLightweightCmp;
    private Integer pathColor; //used by the internalLightweightCmp to know the color to use to draw next paths
    private MapComponent dummyMapComponent; //dummy Mapcomponent (returning transparent tiles) used on top of javascript browsercomponent and allowing to return synchroneously for get methods
    private BrowserComponent internalBrowser;
    private JavascriptContext browserContext;
    private ArrayList<MapListener> listeners;
    private PointsLayer points;
    private Container mapWrapper;
    private Container mapLayoutWrapper;
    
    private ArrayList<MapObject> markers = new ArrayList<MapObject>();
    private static HashMap<Integer, MapContainer> instances = new HashMap<Integer, MapContainer>();
    private static int currentMapId;
    private int mapId;
    private boolean showMyLocation;
    private boolean rotateGestureEnabled;
    
    private EventDispatcher tapListener;
    private EventDispatcher longPressListener;
    
    private static final String BRIDGE="com_codename1_googlemaps_MapContainer_bridge";
    
    /**
     * Default constructor creates an instance with the standard OpenStreetMap version if necessary
     */
    public MapContainer() {
        this(new OpenStreetMapProvider());
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected void initComponent() {
        instances.put(mapId, this);
        super.initComponent();
        if(isNativeMaps()) {
            internalNative.initialize();
            getComponentAt(0).setVisible(true);
        }
        ((MapLayout)mapLayoutWrapper.getLayout()).onInit();
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected void deinitialize() {
        instances.remove(mapId);
        if(internalNative != null) {
            internalNative.deinitialize();
        }
        ((MapLayout)mapLayoutWrapper.getLayout()).onDeinit();
        super.deinitialize();
        
    }
    
    /**
     * Uses the given provider in case of a fallback
     * 
     * @param provider the map provider
     */
    public MapContainer(MapProvider provider) {
        this(provider, null);
    }
    
    /**
     * Uses HTML JavaScript google maps on fallback platforms instead of the tiled map
     * @param javaScriptMapsAPIKey the API key for HTML maps
     */
    public MapContainer(String javaScriptMapsAPIKey) {
        this(null, javaScriptMapsAPIKey);
    }
    
    /**
     * Uses the given provider in case of a fallback
     * 
     * @param provider the map provider
     */
    private MapContainer(MapProvider provider, final String htmlApiKey) {
        super(new LayeredLayout());
        $(this).selectAllStyles().setPadding(0);
        mapWrapper = new Container(new BorderLayout());
        
        add(mapWrapper);
        mapLayoutWrapper = new Container() {
        	public void setShouldLayout(boolean layout) {
        		super.setShouldLayout(layout);
        	}
        };
        mapLayoutWrapper.setScrollableX(false);
        mapLayoutWrapper.setScrollableY(false);
        $(mapWrapper, mapLayoutWrapper).selectAllStyles().setPadding(0).setMargin(0).setBorder(Border.createEmpty());
        MapLayout mapLayout = new MapLayout(this, mapLayoutWrapper);
        mapLayoutWrapper.setLayout(mapLayout);
        add(mapLayoutWrapper);
        if ("true".equals(Display.getInstance().getProperty("MapContainer.debug", "false"))) {
            Log.p("MapContainer debug mode ON");
            debug = true;
        }
        if (provider == null && "win".equals(Display.getInstance().getPlatformName())) {
            // Right now UWP gives an NPE when we use the internal browser
            // so disabling it for now.
        	if (htmlApiKey != null) { provider = new GoogleMapsProvider(htmlApiKey); }
        	else { provider = new OpenStreetMapProvider(); }
        }
        internalNative = (InternalNativeMaps)NativeLookup.create(InternalNativeMaps.class);
        if(internalNative != null) {
            if(internalNative.isSupported()) {
                currentMapId++;
                mapId = currentMapId;
                PeerComponent p = internalNative.createNativeMap(mapId);
                $(p).selectAllStyles().setPadding(0).setMargin(0).setBorder(Border.createEmpty());
                
                // can happen if Google play services failed or aren't installed on an Android device
                if(p != null) {
                    //System.out.println("Adding native map "+p);
                    
                    mapWrapper.addComponent(BorderLayout.CENTER, p);
                    return;
                } else {
                    //System.out.println("Failed to add native map");
                }
            } 
            internalNative = null;
        }
        if(provider != null) {
            internalLightweightCmp = new MapComponent(provider) {
                private boolean drg = false;

                @Override
                public void pointerDragged(int x, int y) {
                    super.pointerDragged(x, y); 
                    drg = true;
                }

                @Override
                public void pointerDragged(int[] x, int[] y) {
                    super.pointerDragged(x, y); 
                    drg = true;
                }

                @Override
                public void pointerReleased(int x, int y) {
                    super.pointerReleased(x, y); 
                    if(!drg) {
                        fireTapEvent(x, y);
                    }
                    drg = false;
                }

                @Override
                public void longPointerPress(int x, int y) {
                    super.longPointerPress(x, y); 
                    fireLongPressEvent(x, y);
                }
            };
            mapWrapper.addComponent(BorderLayout.CENTER, internalLightweightCmp);
        }
        else {
            dummyMapComponent = new MapComponent(new GoogleMapsProvider(htmlApiKey){
                Image img;

                @Override
                public Tile tileFor(BoundingBox bbox) {
                    Dimension size = tileSize();
                    if (img == null) {
                        img = Image.createImage(size.getWidth(), size.getHeight());
                    }
                    return new Tile(size, bbox, img);
                }

                @Override
                public int maxZoomLevel() {
                    return 22; 
                }
            });
            internalBrowser = new BrowserComponent();
            internalBrowser.getAllStyles().setPadding(0,0,0,0);
            internalBrowser.getAllStyles().setMargin(0,0,0,0);

            initBrowserComponent(htmlApiKey);
            
            mapWrapper.addComponent(BorderLayout.CENTER, 
                    $(LayeredLayout.encloseIn(dummyMapComponent, internalBrowser))
                            .selectAllStyles()
                            .setPadding(0)
                            .setMargin(0)
                            .setBorder(Border.createEmpty())
                            .asComponent()
            );
        }
        setRotateGestureEnabled(true);
    }

    private BrowserBridge browserBridge = new BrowserBridge();
    
    /*
    private void checkBridgeReady(final SuccessCallback<Boolean> callback) {
         if (internalBrowser == null) {
             callback.onSucess(false);
             return;
         }
         internalBrowser.execute("callback.onSuccess(window.com_codename1_googlemaps_MapContainer_bridge)", new SuccessCallback<JSRef>() {

             public void onSucess(JSRef value) {
                 if (value.getJSType() == JSType.OBJECT || value.getJSType() == JSType.FUNCTION) {
                     callback.onSucess(true);
                 }
             }
         });
         
     }
    */
    private class BrowserBridge {
        List<Runnable> onReady = new ArrayList<Runnable>();
        boolean ready;
       // private JavascriptContext ctx;
        
        BrowserBridge() {
            
        }
        
        private void ready(Runnable r) {
            if (ready) {
                if (!onReady.isEmpty()) {
                    List<Runnable> tmp = new ArrayList<Runnable>();
                    synchronized(onReady) {
                        tmp.addAll(onReady);
                        onReady.clear();
                    }
                    for (Runnable tr : tmp) {
                        tr.run();
                    }
                }
                if (r != null) {
                    r.run();
                }
            } else {
                if (r == null) {
                    return;
                }
                
                synchronized(onReady) {
                    onReady.add(r);
                }
            }
        }
        
        /*
        private void waitForReady() {
            //System.out.println("Waiting for ready");
            int ctr = 0;
            while (!ready) {
                if (ctr++ > 500) {
                    throw new RuntimeException("Waited too long for browser bridge");
                }
                checkBridgeReady(new SuccessCallback<Boolean>() {
 
                     public void onSucess(Boolean value) {
                         if (value != null && value) {
                             ready = true;
                         }
                     }
                     
                 });
                
            
                Display.getInstance().invokeAndBlock(new Runnable() {

                    public void run() {
                        try {
                            Thread.sleep(20);
                            //System.out.println("Finished sleeping 20-");
                        } catch (Exception ex){
                            Log.e(ex);
                        }
                    }
                    
                });
            }
            
        }
        */
    }
   
    
    private void initBrowserComponent(String htmlApiKey) {
        if (debug) {
            Log.e(new RuntimeException("Initializing Browser Component.  This stack trace is just for tracking purposes.  It is NOT a real exception"));
        }
        //System.out.println("About to check location");
        Location loc = LocationManager.getLocationManager().getLastKnownLocation();
        try {
            //if (true)return;
            //System.out.println("About to load map text");
            String str = Util.readToString(Display.getInstance().getResourceAsStream(null, "/com_codename1_googlemaps_MapContainer.html"));
            //System.out.println("Map text: "+str);
            str = StringUtil.replaceAll(str, "YOUR_API_KEY", htmlApiKey);
            //System.out.println("Finished setting API key");
            str = StringUtil.replaceAll(str, "//origin = MAPCONTAINER_ORIGIN", "origin = {lat: "+ loc.getLatitude() + ", lng: "  + loc.getLongitude() + "};");
            //System.out.println("Finished setting origin");
            internalBrowser.setPage(str, "/");
            if (debug) {
                Log.e(new RuntimeException("Adding onLoad Listener.  This stack trace is just for tracking purposes.  It is NOT a real exception"));
            }
            internalBrowser.addWebEventListener("onLoad", new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if (debug) {
                        Log.e(new RuntimeException("Inside onLoad Listener.  This stack trace is just for tracking purposes.  It is NOT a real exception"));
                    }
                    //JavascriptContext ctx = new JavascriptContext(internalBrowser);
                    //browserBridge.ctx = ctx;
                    internalBrowser.execute("com_codename1_googlemaps_MapContainer = {}");
                    internalBrowser.addJSCallback("com_codename1_googlemaps_MapContainer.fireTapEvent = function(x,y){callback.onSuccess(x+','+y)};", new SuccessCallback<BrowserComponent.JSRef>() {

                        public void onSucess(BrowserComponent.JSRef value) {
                            String[] parts = Util.split(value.getValue(), ",");
                            int x = new Double(Double.parseDouble(parts[0])).intValue();
                            int y = new Double(Double.parseDouble(parts[1])).intValue();
                            fireTapEvent(x, y);
                        }
                    });
                    internalBrowser.addJSCallback("com_codename1_googlemaps_MapContainer.fireLongPressEvent = function(x,y){callback.onSuccess(x+','+y)};", new SuccessCallback<BrowserComponent.JSRef>() {

                        public void onSucess(BrowserComponent.JSRef value) {
                            String[] parts = Util.split(value.getValue(), ",");
                            int x = new Double(Double.parseDouble(parts[0])).intValue();
                            int y = new Double(Double.parseDouble(parts[1])).intValue();
                            fireLongPressEvent(x, y);
                        }
                    });
                    
                    internalBrowser.addJSCallback("com_codename1_googlemaps_MapContainer.fireMapChangeEvent = function(zoom,lat,lon){callback.onSuccess(zoom+','+lat+','+lon)};", new SuccessCallback<BrowserComponent.JSRef>() {

                        public void onSucess(BrowserComponent.JSRef value) {
                            String[] parts = Util.split(value.getValue(), ",");
                            float zoom = Float.parseFloat(parts[0]);
                            double lat  = Double.parseDouble(parts[1]);
                            double lon = Double.parseDouble(parts[2]);
                            fireMapListenerEvent(zoom, lat, lon);
                        }
                    });
                   
                    internalBrowser.addJSCallback("com_codename1_googlemaps_MapContainer.fireMarkerEvent = function(id){callback.onSuccess(id)};", new SuccessCallback<BrowserComponent.JSRef>() {

                        public void onSucess(BrowserComponent.JSRef value) {
                            fireMarkerEvent(value.getInt());
                        }
                    });
                    internalBrowser.execute("callback.onSuccess(com_codename1_googlemaps_MapContainer_bridge)", new SuccessCallback<BrowserComponent.JSRef>() {

                        public void onSucess(BrowserComponent.JSRef value) {
                            if ("null".equals(value.getValue()) || value.getJSType() == BrowserComponent.JSType.UNDEFINED) {
                                internalBrowser.execute("com_codename1_googlemaps_MapContainer_onReady=function(bridge){callback.onSuccess(bridge)};", new SuccessCallback<BrowserComponent.JSRef>() {
                                    public void onSucess(BrowserComponent.JSRef value) {
                                        browserBridge.ready = true;
                                        syncBrowserAndDummyDefaultValues();
                                        browserBridge.ready(null);
                                    }
                                });
                            } else {
                                browserBridge.ready = true;
                                syncBrowserAndDummyDefaultValues();
                                browserBridge.ready(null);
                            }
                        }
                    });
                    //internalBrowser.execute("try{google.maps.event.trigger(com_codename1_googlemaps_MapContainer_bridge.map, \"resize\");} catch(e){}");
                
                    ///System.out.println("Bridge is ready");
                    if (debug) {
                        Log.p("About to fire browserBridge.ready(null) event to kick things off");
                    }
                    browserBridge.ready(null);
                }
            });
            
            
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    
    private void syncBrowserAndDummyDefaultValues() {
    	internalBrowser.execute(BRIDGE+".setMapType(${0})", new Object[]{dummyType}, jsres->{});
    	internalBrowser.execute(BRIDGE+".setCameraPosition(${0}, ${1})", new Object[]{dummyMapComponent.getCenter().getLatitude(), dummyMapComponent.getCenter().getLongitude()});
    	internalBrowser.execute(BRIDGE+".setMinZoom(${0})", new Object[]{dummyMapComponent.getMinZoomLevel()});
    	internalBrowser.execute(BRIDGE+".setMaxZoom(${0})", new Object[]{dummyMapComponent.getMaxZoomLevel()});
    	internalBrowser.execute(BRIDGE+".setZoom(${0})", new Object[]{dummyMapComponent.getZoomLevel()});
    }
        
    
    static void mapUpdated(int mapId) {
        final MapContainer mc = instances.get(mapId);
        if(mc != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    mc.repaint();
                }
            });
        }
    }
    
    /**
     * Returns true if native maps are used
     * @return false if the lightweight maps are used
     */
    public boolean isNativeMaps() {
        return internalNative != null;
    }
    
    private int dummyType = MAP_TYPE_NORMAL;
    
    /**
     * Sets the native map type to one of the MAP_TYPE constants
     * @param type one of the MAP_TYPE constants
     */
    public void setMapType(int type) {
        if(internalNative != null) {
            internalNative.setMapType(type);
        } else {
            if (internalLightweightCmp != null) {
            	if (internalLightweightCmp.getProvider() instanceof GoogleMapsProvider) {
            		((GoogleMapsProvider)internalLightweightCmp.getProvider()).setMapType(type); //TODO: unify GoogleMapsProvider and MapContainer map type constants... 
            	}
            } else {
                // browser component
                //browserBridge.waitForReady();
                //browserBridge.bridge.call("setMapType", new Object[]{type});
                dummyType = type;
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setMapType(${0})", new Object[]{type}, jsres->{});
                });
                
            }
        }
    }
    
    /**
     * Returns the native map type
     * @return one of the MAP_TYPE constants
     */
    public int getMapType() {
        if(internalNative != null) {
            return internalNative.getMapType();
        } 
        else {
        	if (internalLightweightCmp != null) {
            	if (internalLightweightCmp.getProvider() instanceof GoogleMapsProvider) {
            		return ((GoogleMapsProvider)internalLightweightCmp.getProvider()).getMapType(); //TODO: unify GoogleMapsProvider and MapContainer map type constants... 
            	}
        	}
        	else { //if (browserBridge != null) {
        		//browserBridge.waitForReady();
        		//return browserBridge.bridge.callInt("getMapType");
        		return dummyType;
        		//return internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getMapType())").getInt();
        	}     
        }
        return MAP_TYPE_NORMAL;
    }
    
    
    /**
     * Sets the native map style 
     * @param json the json google map style object
     */
    public void setMapStyle(String json) {
    	 if(internalNative != null) {
             internalNative.setMapStyle(json);
         } else {
             if (internalLightweightCmp != null) {
            	 if (internalLightweightCmp.getProvider() instanceof GoogleMapsProvider) {
             		((GoogleMapsProvider)internalLightweightCmp.getProvider()).setMapStyle(json); 
             	}
             } else {
                 browserBridge.ready(()->{
                     internalBrowser.execute(BRIDGE+".setMapStyle("+json+")", jsres->{});
                 });
                 
             }
         }
    }
    
    
    
    
    
    static void fireMarkerEvent(int mapId, final long markerId) {
        final MapContainer mc = instances.get(mapId);
        if(mc != null) {
            if(!Display.getInstance().isEdt()) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        mc.fireMarkerEvent(markerId);
                    }
                });
                return;
            }
            mc.fireMarkerEvent(markerId);
        }
    }
    
    void fireMarkerEvent(long markerId) {
        for(MapObject m : markers) {
            if(m.mapKey == markerId) {
                if(m.callback != null) {
                    m.callback.actionPerformed(new ActionEvent(m));
                }
                return;
            }
        }
    }
    
    /**
     * A class to encapsulate parameters for adding markers to map.
     */
    public static class MarkerOptions {
        private final EncodedImage icon;
        private final LatLng location;
        private String text="";
        private String longText="";
        private ActionListener onClick;
        private float anchorU = 0.5f;
        private float anchorV = 1f;
        
        public MarkerOptions(LatLng coord, EncodedImage icon) {
            this.location = coord;
            this.icon = icon;

        }


        /**
         * Gets the icon image for this marker.
         * @return the icon
         */
        public EncodedImage getIcon() {
            return icon;
        }

        /**
         * Gets the location of this marker
         * @return the location
         */
        public LatLng getLocation() {
            return location;
        }

        /**
         * Returns the text for this marker.
         * @return the text
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the text for this marker
         * @param text the text to set
         * @return Self for chaining
         */
        public MarkerOptions text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Gets the long text for this marker.
         * @return the longText
         */
        public String getLongText() {
            return longText;
        }

        /**
         * Sets the long text for this marker.
         * @param longText the longText to set
         * @return Self for chaining.
         */
        public MarkerOptions longText(String longText) {
            this.longText = longText;
            return this;
        }

        /**
         * Gets the onclick handler for this marker.
         * @return the onClick
         */
        public ActionListener getOnClick() {
            return onClick;
        }

        /**
         * Sets the onclick handler for this marker.
         * @param onClick the onClick to set
         * @return Self for chaining.
         */
        public MarkerOptions onClick(ActionListener onClick) {
            this.onClick = onClick;
            return this;
        }

        /**
         * Gets the horizontal alignment of this marker in (u,v) coordinates.  
         * 0.0 = align left edge with coord. 0.5 = align center.  1.0 = align right edge with coord.
         * @return the anchorU
         */
        public float getAnchorU() {
            return anchorU;
        }

        /**
         * Sets the horizontal alignment of this marker in (u,v) coordinates.
         * 0.0 = align left edge with coord. 0.5 = align center.  1.0 = align right edge with coord.
         * @param anchorU the anchorU to set
         */
        public MarkerOptions anchorU(float anchorU) {
            this.anchorU = anchorU;
            return this;
        }

        /**
         * Gets the vertical alignment of this marker in (u,v) coordinates.
         * 0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
         * @return the anchorV
         */
        public float getAnchorV() {
            return anchorV;
        }

        /**
         * Sets the vertical alignment of this marker in (u,v) coordinates.
         * 0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
         * @param anchorV the anchorV to set
         */
        public MarkerOptions anchorV(float anchorV) {
            this.anchorV = anchorV;
            return this;
        }
        
        /**
         * Sets the horizontal and vertical alignments of this marker in (u,v) coordinates.
         * @param anchorU 0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
         * @param anchorV 0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
         * @return Self for chaining.
         */
        public MarkerOptions anchor(float anchorU, float anchorV) {
            this.anchorU = anchorU;
            this.anchorV = anchorV;
            return this;
        }
    }
    
    /**
     * Adds a component as a marker on the map.
     * @param marker The component to be placed on the map.
     * @param location The location of marker.
     * @param anchorU The horizontal alignment of the marker. 0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
     * @param anchorV The vertical alignment of the marker.  0.0 = align top edge with coord. 0.5 = align center.  1.0 = align bottom edge with coord.
     * @return A MapObject that can be used for later removing the marker.
     */
    public MapObject addMarker(Component marker, LatLng location, float anchorU, float anchorV) {
        mapLayoutWrapper.add(location, marker);
        MapLayout.setHorizontalAlignment(marker, anchorU);
        MapLayout.setVerticalAlignment(marker, anchorV);
        MapObject o = new MapObject();
        o.componentMarker = marker;
        return o;
        
    }
    
    /**
     * Adds a component as a marker on the map with default horizontal/vertical alignment (0.5, 1.0)
     * @param marker The component to add as a marker.
     * @param location The location of the marker.
     * @return A MapObject that can be used for later removing the marker.
     */
    public MapObject addMarker(Component marker, LatLng location) {
        return addMarker(marker, location, 0.5f, 1f);
    }
    
    /**
     * Adds a marker to the map with the given attributes
     * @param icon the icon, if the native maps are used this value can be null to use the default marker
     * @param location the coordinate for the marker
     * @param text the string associated with the location
     * @param longText longer description associated with the location
     * @param onClick will be invoked when the user clicks the marker. Important: events are only sent when the native map is in initialized state
     * @return marker reference object that should be used when removing the marker 
     */
    public MapObject addMarker(EncodedImage icon, LatLng location, String text, String longText, final ActionListener onClick) {
        return addMarker(new MarkerOptions(location, icon)
                .text(text)
                .longText(longText)
                .onClick(onClick));
        
    }
        
    /**
     * Adds a marker to the map with the given attributes
     * @param opts The marker options.
     * @return marker reference object that should be used when removing the marker
     */
    public MapObject addMarker(MarkerOptions opts) {
        //public MapObject addMarker(EncodedImage icon, Coord location, String text, String longText, final ActionListener onClick) {
        EncodedImage icon = opts.getIcon();
        LatLng location = opts.getLocation();
        String text = opts.getText();
        String longText = opts.getLongText();
        ActionListener onClick = opts.getOnClick();
        if(internalNative != null) {
            byte[] iconData = null;
            if(icon != null) {
                iconData = icon.getImageData();
                internalNative.setMarkerSize(icon.getWidth(), icon.getHeight());
            }
            
            long key = internalNative.addMarker(iconData, location.getLatitude(), location.getLongitude(), text, longText, onClick != null, 
                    opts.anchorU, 
                    opts.anchorV
            );
            MapObject o = new MapObject();
            o.mapKey = key;
            o.callback = onClick;
            markers.add(o);
            return o;
        } else {
            if(internalLightweightCmp != null) {
                PointLayer pl = new PointLayer(new Coord(location.getLatitude(), location.getLongitude()), text, icon);
                if(points == null) {
                    points = new PointsLayer();
                    internalLightweightCmp.addLayer(points);
                }
                points.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        PointLayer point = (PointLayer)evt.getSource();
                        for(MapObject o : markers) {
                            if(o.point == point) {
                                if(o.callback != null) {
                                    o.callback.actionPerformed(new ActionEvent(o));
                                }
                                evt.consume();
                                return;
                            }
                        }
                    }
                });
                points.addPoint(pl);
                MapObject o = new MapObject();
                o.point = pl;
                o.callback = onClick;
                markers.add(o);
                internalLightweightCmp.revalidate();
                return o;
                
            } else {
                
                String uri = null;
                int iconWidth = 0;
                int iconHeight = 0;
                int anchorU = 0;
                int anchorV = 0;
                if(icon != null) {
                    uri = WebBrowser.createDataURI(icon.getImageData(), "image/png");
                    iconWidth = icon.getWidth();
                    iconHeight = icon.getHeight();
                    anchorU = (int)(icon.getWidth() * opts.anchorU);
                    anchorV = (int)(icon.getHeight() * opts.anchorV);
                } 
                //browserBridge.waitForReady();
                
                MapObject o = new MapObject();
                o.callback = onClick;
                o.pending = true;
                final String fUri = uri;
                final int fIconWidth = iconWidth;
                final int fIconHeight = iconHeight;
                final float fAnchorU = anchorU;
                final float fAnchorV = anchorV;
                browserBridge.ready(()->{
                    internalBrowser.execute(
                            "callback.onSuccess("+BRIDGE+".addMarker(${0}, ${1}, ${2}, ${3}, ${4}, ${5}, ${6}, ${7}, ${8}))", new Object[]{

                    //long key = ((Double)browserBridge.bridge.call("addMarker", new Object[]{
                        fUri,
                        location.getLatitude(),
                        location.getLongitude(),
                        text,
                        longText,
                        fIconWidth,
                        fIconHeight, 
                        fAnchorU,
                        fAnchorV},
                            jsres->{
                                o.mapKey = jsres.getInt();
                                o.pending = false;

                            }
                    //})).intValue();
                    );
                });
                
                //MapObject o = new MapObject();
                
                //o.mapKey = res.getInt();
                //o.callback = onClick;
                markers.add(o);
                //System.out.println("MapKey added "+o.mapKey);
                return o;
            }
        }
    }
    
    /**
     * Removes a component marker that was previously added using {@link #addMarker(com.codename1.ui.Component, com.codename1.maps.Coord) }
     * @param marker 
     */
    public void removeMarker(MapObject marker) {
        if (markers.remove(marker)) {
        	mapLayoutWrapper.removeComponent(marker.componentMarker);
        }
    }    
    

    public void removeMarker(Component marker) {
    	if (marker!=null) {
    		Iterator<MapObject> tpit = markers.iterator();
    		while (tpit.hasNext()) {
    			MapObject mo = tpit.next();
    			if (marker.equals(mo.componentMarker)) {
    				tpit.remove();
    				mapLayoutWrapper.removeComponent(marker);
    			}
    		}
    	}
    }
    
    /**
     * Update the location of a marker previously added to the map
     * @param marker
     * @param location 
     */
    public void updateMarkerLocation(Component marker, LatLng location) {
        if (marker.getParent() != mapLayoutWrapper){
            addMarker(marker, location);
        }
        else {
            mapLayoutWrapper.getLayout().addLayoutComponent(location, marker, mapLayoutWrapper);
            mapLayoutWrapper.revalidate();
            //mapLayoutWrapper.setShouldLayout(true);
        }
    }
    
    
    /** Set the color to use for next paths. Seting this to null restore the platform's default path color */
    public void setPathColor(Integer argb) {
    	 if(internalNative != null) {
             if (argb!=null){internalNative.setPathColor(argb);}
             else {internalNative.restorePathDefaultColor();}
         } else {
             if (internalLightweightCmp != null) {
            	 pathColor = argb;
             } else {
                // browserBridge.ready(()->{
                     internalBrowser.execute(BRIDGE+".setPathColor("+((argb==null)?"":argb)+")", jsres->{});
                // });
            }
         }
    }
    
    /** Set the thickness to use for next paths. Seting this to null restore the platform's default path color */
    public void setPathThickness(Integer thickness) {
    	 if(internalNative != null) {
             if (thickness!=null){internalNative.setPathThickness(thickness);}
             else {internalNative.restorePathDefaultThickness();}
         } else {
             if (internalLightweightCmp != null) {
            	//TODO. Unsupported yet
             } else {
                // browserBridge.ready(()->{
                     internalBrowser.execute(BRIDGE+".setPathThickness("+((thickness==null)?"":thickness)+")", jsres->{});
                // });
            }
         }
    }
    
    
    /** Set the thickness to use for next paths. Seting this to null restore the platform's default path color */
    public void setPathGeodesic(Boolean geodesic) {
    	 if(internalNative != null) {
             if (geodesic!=null){internalNative.setPathGeodesic(geodesic);}
             else {internalNative.restorePathDefaultGeodesic();}
         } else {
             if (internalLightweightCmp != null) {
            	//TODO. Unsupported yet
             } else {
                // browserBridge.ready(()->{
                     internalBrowser.execute(BRIDGE+".setPathGeodesic("+((geodesic==null)?"":geodesic)+")", jsres->{});
                // });
            }
         }
    }
    
    
    /**
     * Draws a path on the map
     * @param path the path to draw on the map
     * @return a map object instance that allows us to remove the drawn path
     */
    public MapObject addPath(LatLng... path) {
        if(internalNative != null) {
            long key = internalNative.beginPath();
            for(LatLng c : path) {
                internalNative.addToPath(key, c.getLatitude(), c.getLongitude());
            }
            key = internalNative.finishPath(key);
            MapObject o = new MapObject();
            o.mapKey = key;
            markers.add(o);
            return o;
        } else {
            if(internalLightweightCmp != null) {
                LinesLayer ll = new LinesLayer();
                if (pathColor!=null) {ll.lineColor(pathColor);}
                Coord[] coords = new Coord[path.length];
                for(int i=0; i<path.length; i++) {
                	LatLng pos = path[i];
                	coords[i] = new Coord(pos.getLatitude(), pos.getLongitude());
                }
                ll.addLineSegment(coords);

                internalLightweightCmp.addLayer(ll);
                MapObject o = new MapObject();
                o.lines = ll;
                markers.add(o);
                return o;
            } else {
                //browserBridge.waitForReady();
                StringBuilder json = new StringBuilder();
                json.append("[");
                boolean first = true;
                for(LatLng c : path) {
                    if (first) {
                        first = false;
                    } else {
                        json.append(", ");
                    }
                    json.append("{\"lat\":").append(c.getLatitude()).append(", \"lon\": ").append(c.getLongitude()).append("}");
                }
                json.append("]");
                //long key = ((Double)browserBridge.bridge.call("addPathAsJSON", new Object[]{json.toString()})).intValue();
                MapObject o = new MapObject();
                o.pending = true;
                browserBridge.ready(()->{
                    internalBrowser.execute("callback.onSuccess("+BRIDGE+".addPathAsJSON(${0}));", new Object[]{json.toString()}, jsres->{
                        o.mapKey = jsres.getInt();
                        o.pending = false;
                    });
                });
                
                markers.add(o);
                return o;
            }
        }
    }
    
    /**
     * Removes the map object from the map
     * @param obj the map object to remove
     */
    public void removeMapObject(MapObject obj) {
        if (obj.componentMarker != null) {
            removeMarker(obj);
            return;
        }
        markers.remove(obj);
        if(internalNative != null) {
            internalNative.removeMapElement(obj.mapKey);
        } else {
            if(obj.lines != null) {
                if(internalLightweightCmp != null) {
                    internalLightweightCmp.removeLayer(obj.lines);
                } else {
                    //browserBridge.waitForReady();
                    //browserBridge.bridge.call("removeMapElement", new Object[]{obj.mapKey});
                    if (obj.pending) {
                        Display.getInstance().callSeriallyOnIdle(()->{
                            removeMapObject(obj);
                        });
                        return;
                    }
                    browserBridge.ready(()->{
                        internalBrowser.execute("callback.onSuccess("+BRIDGE+".removeMapElement(${0}))", new Object[]{obj.mapKey}, jsres->{
                            // do nothing here.
                        });
                    });
                    
                    
                }
            } else {
                if(internalLightweightCmp != null) {
                    if (points != null) {
                        points.removePoint(obj.point);
                    }
                } else {
                    //browserBridge.waitForReady();
                   // browserBridge.bridge.call("removeMapElement", new Object[]{obj.mapKey});
                    if (obj.pending) {
                       Display.getInstance().callSeriallyOnIdle(()->{
                            removeMapObject(obj);
                        });
                        return;
                    }
                    browserBridge.ready(()->{
                        internalBrowser.execute("callback.onSuccess("+BRIDGE+".removeMapElement(${0}))", new Object[]{obj.mapKey}, jsres->{
                            // Do nothing

                        });
                    });
                }
            }
        }
    }
    
    /**
     * Removes all the layers from the map
     */
    public void clearMapLayers() {
        mapLayoutWrapper.removeAll();
        if(internalNative != null) {
            internalNative.removeAllMarkers();
            markers.clear();
        } else {
            if(internalLightweightCmp != null) {
                internalLightweightCmp.removeAllLayers();
                points = null;
            } else {
                //browserBridge.waitForReady();
                //browserBridge.bridge.call("removeAllMarkers");
                browserBridge.ready(()->{
                    internalBrowser.execute("callback.onSuccess("+BRIDGE+".removeAllMarkers())", jsres->{});
                });
                
                markers.clear();
            }
        }
    }
    
    
    
    /**
     * Position the map camera
     * @param crd the coordinate
     */
    public void setCameraPosition(Coord crd) {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                internalLightweightCmp.zoomTo(crd, internalLightweightCmp.getZoomLevel());
            } else {
                //browserBridge.waitForReady();
                //browserBridge.bridge.call(
                //        "setCameraPosition", 
                //        new Object[]{crd.getLatitude(), crd.getLongitude()}
                //);
                dummyMapComponent.zoomTo(crd, Math.round(getZoom()));
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setCameraPosition(${0}, ${1})", new Object[]{crd.getLatitude(), crd.getLongitude()});
                });
            }
            return;
        }
        internalNative.setPosition(crd.getLatitude(), crd.getLongitude());
    }
    
    /**
     * Returns the position in the center of the camera
     * @return the position
     */
    public Coord getCameraPosition() {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                return internalLightweightCmp.getCenter();
            } else {
                //browserBridge.waitForReady();
                //String pos = browserBridge.bridge.callString("getCameraPosition");
                return dummyMapComponent.getCenter();
                /*
                String pos = internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getCameraPosition())").toString();
                try {
                    String latStr = pos.substring(0, pos.indexOf(" "));
                    String lnStr = pos.substring(pos.indexOf(" ")+1);
                    return new Coord(Double.parseDouble(latStr), Double.parseDouble(lnStr));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new Coord(0, 0);
                }
                */
            }
        }
        return new Coord(internalNative.getLatitude(), internalNative.getLongitude());
    }
    
    /**
     * Zoom to the given level
     * @param zoom the zoom level
     */
    public void setZoom(float zoom) {
        if(internalNative != null) {
            internalNative.setZoom(zoom);
        } else {
            //javascript or lightweighted map components do not support float zoom values. So convert it to int
            int izoom = Math.round(zoom);
            if(internalLightweightCmp != null) {
                internalLightweightCmp.setZoomLevel(izoom);
            } else {
                //browserBridge.waitForReady();
                //browserBridge.bridge.call("zoom", new Object[]{ crd.getLatitude(), crd.getLongitude(), zoom});
                dummyMapComponent.setZoomLevel(izoom);
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setZoom(${0})", new Object[]{izoom});
                });
            }
        }
    }
    
    /**
     * Returns the current zoom level
     * @return the current zoom level between min/max zoom
     */
    public float getZoom() {
        if(internalNative != null) {
            return internalNative.getZoom();
        } else {
            if(internalLightweightCmp != null) {
                return internalLightweightCmp.getZoomLevel();
            }
            //browserBridge.waitForReady();
            //return browserBridge.bridge.callInt("getZoom");
            return dummyMapComponent.getZoomLevel();
            //return internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getZoom())").getInt();
            
        }        
    }
    
    
    /**
     * set the maximum zoom level
     * @param zoom the zoom level
     */
    public void setMaxZoom(float zoom) {
        if(internalNative != null) {
            internalNative.setMaxZoom(zoom);
        } else {
            //javascript or lightweighted map components do not support float zoom values. So convert it to int
            int izoom = (int) Math.ceil(zoom);
            if(internalLightweightCmp != null) {
                internalLightweightCmp.setMaxZoomLevel(izoom);
            } else {
                //browserBridge.waitForReady();
                //browserBridge.bridge.call("zoom", new Object[]{ crd.getLatitude(), crd.getLongitude(), zoom});
                dummyMapComponent.setMaxZoomLevel(izoom);
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setMaxZoom(${0})", new Object[]{izoom});
                });
            }
        }
    }
    
     /**
     * Returns the max zoom level of the map
     *
     * @return max zoom level
     */
    public float getMaxZoom() {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                return internalLightweightCmp.getMaxZoomLevel();
            } else {
                return dummyMapComponent.getMaxZoomLevel();
                //return browserBridge.bridge.callInt("getMaxZoom");
                //return internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getMaxZoom())").getInt();
            }
        }
        return internalNative.getMaxZoom();
    }
    
    
    
    /**
     * set the maximum zoom level
     * @param zoom the zoom level
     */
    public void setMinZoom(float zoom) {
        if(internalNative != null) {
            internalNative.setMinZoom(zoom);
        } else {
            //javascript or lightweighted map components do not support float zoom values. So convert it to int
            int izoom = (int) Math.floor(zoom);
            if(internalLightweightCmp != null) {
                internalLightweightCmp.setMinZoomLevel(izoom);
            } else {
                //browserBridge.waitForReady();
                //browserBridge.bridge.call("zoom", new Object[]{ crd.getLatitude(), crd.getLongitude(), zoom});
                dummyMapComponent.setMinZoomLevel(izoom);
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setMinZoom(${0})", new Object[]{izoom});
                });
            }
        }
    }
    
    
    /**
     * Returns the min zoom level of the map
     *
     * @return min zoom level
     */
    public float getMinZoom() {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                return internalLightweightCmp.getMinZoomLevel();
            } else {
                //browserBridge.waitForReady();
                return dummyMapComponent.getMinZoomLevel();
                //return internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getMinZoom())").getInt();
            }
        }
        return internalNative.getMinZoom();
    }
    
    /**
     * Zoom to the given coordinate on the map
     * @param crd the coordinate
     * @param zoom the zoom level
     */
    public void setCamera(Coord crd, float zoom) {
        if(internalNative != null) {
            internalNative.setCamera(crd.getLatitude(), crd.getLongitude(), zoom);
        } else {
            //javascript or lightweighted map components do not support float zoom values. So convert it to int
            int izoom = Math.round(zoom);
            if(internalLightweightCmp != null) {
                internalLightweightCmp.zoomTo(crd, izoom);
            } else {
                dummyMapComponent.zoomTo(crd, izoom);
                browserBridge.ready(()->{
                    internalBrowser.execute(BRIDGE+".setCamera(${0}, ${1}, ${2})", new Object[]{ crd.getLatitude(), crd.getLongitude(), izoom});
                });
            }
        }
    }
    
    /**
     * Pans and zooms to fit the given bounding box.
     * @param bounds The bounding box to display.
     */
    public void fitBounds_old(BoundingBox bounds) {
        Coord c = new Coord(
                (bounds.getNorthEast().getLatitude() + bounds.getSouthWest().getLatitude())/2,
                (bounds.getNorthEast().getLongitude() + bounds.getSouthWest().getLongitude())/2
        );
        double currZoom = getZoom();
        BoundingBox currBbox = getBoundingBox();
        Coord currC = new Coord(
                (currBbox.getNorthEast().getLatitude() + currBbox.getSouthWest().getLatitude())/2,
                (currBbox.getNorthEast().getLongitude() + currBbox.getSouthWest().getLongitude())/2
        );
        
        double currMetersPerPx = 156543.03392 * Math.cos(currC.getLatitude() * Math.PI / 180) / MathUtil.pow(2, currZoom);
        double targetMetersPerPx = 156543.03392 * Math.cos(c.getLatitude() * Math.PI / 180) / MathUtil.pow(2, currZoom);
        double adjustmentFactor = targetMetersPerPx / currMetersPerPx;
        //Log.p("Adjustment factor ="+adjustmentFactor);
        
        Mercator proj = new Mercator();
        BoundingBox currProjected = proj.fromWGS84(currBbox);
        BoundingBox targetProjected = proj.fromWGS84(bounds);
        
        double zoom = currZoom;
        double currLatDiff = Math.abs(currProjected.latitudeDifference());
        double currLngDiff = Math.abs(currProjected.longitudeDifference());
        
        if (currLatDiff == 0) {
            currLatDiff = currMetersPerPx * getHeight();
        }
        if (currLatDiff == 0) {
            currLatDiff = currMetersPerPx * Display.getInstance().getDisplayHeight();
        }
        if (currLngDiff == 0) {
            currLngDiff = currMetersPerPx * getWidth();
        }
        if (currLngDiff == 0) {
            currLngDiff = currMetersPerPx * Display.getInstance().getDisplayWidth();
        }
        
        
        double targetLatDiff = Math.max(Math.abs(targetProjected.latitudeDifference()), 0.0001);
        double targetLngDiff = Math.max(Math.abs(targetProjected.longitudeDifference()), 0.0001);
        
        double latDiff = currLatDiff;
        double lngDiff = currLngDiff;
        //Log.p("LatDiff="+latDiff+", LngDiff="+lngDiff+", targetLatDiff="+targetLatDiff+", targetLngDiff="+targetLngDiff+", adjustmentFactor="+adjustmentFactor);
        //Log.p("zoom="+zoom);
        
        while (targetLatDiff <  latDiff && targetLngDiff * adjustmentFactor < lngDiff) {
            zoom += 1.0;
            latDiff /=2.0;
            lngDiff /=2.0;
        }
        //Log.p("Finished zooming in");
        while (targetLatDiff > latDiff || targetLngDiff * adjustmentFactor > lngDiff) {
            zoom -= 1.0;
            latDiff *= 2.0;
            lngDiff *= 2.0;
            //Log.p("latDiff now="+latDiff+", lngDiff now = "+lngDiff);
        }
        //Log.p("Finished zooming out");
        //Log.p("After: latDiff="+latDiff+", lngDiff="+lngDiff+", zoom="+Math.floor(zoom));
        
        setCamera(c, (int)Math.floor(zoom));
        //setCameraPosition(c);
        //Log.p("Setting center to "+c);
        //Log.p("In order to fit bounds "+bounds);
        /*
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Coord center = MapContainer.this.getCameraPosition();
                        Log.p("New Center is "+center);
                        Log.p("New bbox is "+getBoundingBox());
                    }
                });
            }

        }, 1000);
        */
    }
    
    /**
    * Pans and zooms to fit the given bounding box.
    * Remarq: this is constarint by the min and max map zoom level parameters. So the fit might only be partial if the min zoom parameter is too high to fit the whole desired boundingbox
    * @param bounds The bounding box to display.
    */
    public void fitBounds(BoundingBox bounds) {
        Coord c = new Coord(
                (bounds.getNorthEast().getLatitude() + bounds.getSouthWest().getLatitude())/2,
                (bounds.getNorthEast().getLongitude() + bounds.getSouthWest().getLongitude())/2
        );
        float zoom = getZoomLevelToFit(bounds);
        zoom = Math.max(zoom, getMinZoom());
        zoom = Math.min(zoom, getMaxZoom());
        setCamera(c, (int)zoom);
    }
    
    
    public float getZoomLevelToFit(BoundingBox bounds) 
	{
    	float z = 0;
    	
    	double currZoom = getZoom();
	    BoundingBox currBbox = getBoundingBox();
	    
	    double curr_lat = (currBbox.getNorthEast().getLatitude() + currBbox.getSouthWest().getLatitude())/2;
	    double target_lat = (bounds.getNorthEast().getLatitude() + bounds.getSouthWest().getLatitude())/2;
	    
	    double lat_zoom = currZoom - MathUtil.log(Math.abs(bounds.latitudeDifference())/Math.abs(currBbox.latitudeDifference()))/MathUtil.log(2);
	    double lng_zoom = currZoom - MathUtil.log((Math.cos(target_lat* Math.PI/180)*Math.abs(bounds.longitudeDifference()))/(Math.cos(curr_lat*Math.PI/180)*Math.abs(currBbox.longitudeDifference())))/MathUtil.log(2);
	    
	    z = (float) Math.min(lat_zoom, lng_zoom);
	
		return z;
	}
    

    public BoundingBox getBoundingBox() {
        Coord sw = this.getCoordAtPosition(0, getHeight());
        Coord ne = this.getCoordAtPosition(getWidth(), 0);
        return new BoundingBox(sw, ne);
        
    }
    
    
    
     /**
     * Show my location is a feature of the native maps only that allows marking
     * a users location on the map with a circle
     * @return the showMyLocation
     */
    public boolean isShowMyLocation() {
        return showMyLocation;
    }

    /**
     * Show my location is a feature of the native maps only that allows marking
     * a users location on the map with a circle
     * @param showMyLocation the showMyLocation to set
     */
    public void setShowMyLocation(boolean showMyLocation) {
        this.showMyLocation = showMyLocation;
        if(isNativeMaps()) {
            internalNative.setMyLocationEnabled(showMyLocation);
        }
    }
    
    
    public void disableDefaultUI(){
        if(internalNative != null) {
            internalNative.disableDefaultUI();
        } else {
            if(internalLightweightCmp != null) {
                //nothing to do
            } else {
                browserBridge.ready(()->{
                    internalBrowser.execute("callback.onSuccess("+BRIDGE+".disableDefaultUI(true))", jsres->{});
                });
            }
        }
    }
    

    /**
     * @return the rotateGestureEnabled
     */
    public boolean isRotateGestureEnabled() {
        return rotateGestureEnabled;
    }

    /**
     * @param rotateGestureEnabled the rotateGestureEnabled to set
     */
    public final void setRotateGestureEnabled(boolean rotateGestureEnabled) {
        this.rotateGestureEnabled = rotateGestureEnabled;
        if(isNativeMaps()) {
            internalNative.setRotateGesturesEnabled(rotateGestureEnabled);
        }
    }
    
    
    public void disableAllGestures() {
        if(internalNative != null) {
            internalNative.setAllGesturesEnabled(false);
        } else {
            if(internalLightweightCmp != null) {
                internalLightweightCmp.setEnabled(false); //TODO: check how to disable a CN1 component
            } else {
                dummyMapComponent.setEnabled(false); //TODO: check how to disable a CN1 component
                browserBridge.ready(()->{
                    internalBrowser.execute("callback.onSuccess("+BRIDGE+".setAllGesturesEnabled(false))", jsres->{});
                });
            }
        }
    }
    
    
    /**
     * Returns the lat/lon coordinate at the given x/y position
     * @param x the x position in component relative coordinate system
     * @param y the y position in component relative coordinate system
     * @return a lat/lon coordinate
     */
    public Coord getCoordAtPosition(int x, int y) {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                return internalLightweightCmp.getCoordFromPosition(x + getAbsoluteX(), y + getAbsoluteY());
            }
            return dummyMapComponent.getCoordFromPosition(x + getAbsoluteX(), y + getAbsoluteY());
            //browserBridge.waitForReady();
            //x -= internalBrowser.getAbsoluteX();
            //y -= internalBrowser.getAbsoluteY();
            //System.out.println("Browser bridge pointer here is "+browserBridge.bridge.toJSPointer());
            //Object res = browserBridge.bridge.call("getCoordAtPosition", new Object[]{x, y});
            //if (res instanceof Double) {
            //    int i = 0;
            //}
            //String coord = (String)browserBridge.bridge.call("getCoordAtPosition", new Object[]{x, y});
            /*
            String coord = internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getCoordAtPosition(${0}, ${1}))", x, y).toString();
            try {
                String xStr = coord.substring(0, coord.indexOf(" "));
                String yStr = coord.substring(coord.indexOf(" ")+1);
                return new Coord(Double.parseDouble(xStr), Double.parseDouble(yStr));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return new Coord(0, 0);
            */
        }
        internalNative.calcLatLongPosition(x, y);
        return new Coord(internalNative.getScreenLat(), internalNative.getScreenLon());
    }
    
    /**
     * Returns the screen position for the coordinate in component relative position
     * @param lat the latitude
     * @param lon the longitude
     * @return the x/y position in component relative position
     */
    public Point getScreenCoordinate(double lat, double lon) {
        if(internalNative == null) {
            if(internalLightweightCmp != null) {
                Point p =  internalLightweightCmp.getPointFromCoord(new Coord(lat, lon));
                p.setX(p.getX());
                p.setY(p.getY());
                return p;
            }
            Point p =  dummyMapComponent.getPointFromCoord(new Coord(lat, lon));
            p.setX(p.getX());
            p.setY(p.getY());
            return p;
            //browserBridge.waitForReady();
            //String coord = (String)browserBridge.bridge.call("getScreenCoord", new Object[]{lat, lon});
            /*
            String coord = (String)internalBrowser.executeAndWait("callback.onSuccess("+BRIDGE+".getScreenCoord(${0}, ${1}))", new Object[]{lat, lon}).toString();
            try {
                String xStr = coord.substring(0, coord.indexOf(" "));
                String yStr = coord.substring(coord.indexOf(" ")+1);
                Point out =  new Point(
                        (int)Double.parseDouble(xStr), 
                        (int)Double.parseDouble(yStr)
                );
                //out.setX(out.getX() + getAbsoluteX());
                //out.setY(out.getY() + getAbsoluteY());
                return out;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            return new Point(0, 0);
            */
        }
        internalNative.calcScreenPosition(lat, lon);
        return new Point(internalNative.getScreenX() + getStyle().getPaddingLeft(false), internalNative.getScreenY() + getStyle().getPaddingTop());
    }
    
    /**
     * Returns the location on the screen for the given coordinate
     * @param c the coordinate
     * @return the x/y position in component relative position
     */
    public Point getScreenCoordinate(LatLng c) {
        return getScreenCoordinate(c.getLatitude(), c.getLongitude());
    }

    /**
     * Returns the screen points for a list of coordinates.  This is likely more efficient
     * than calling {@link #getScreenCoordinate(com.codename1.maps.Coord) } for each coordinate
     * in the list because this only involves a single call to the native layer.
     * @param coords The coordinates to convert to points.
     * @return A list of points relative to (0,0) of the map container.
     */
    public List<Point> getScreenCoordinates(List<LatLng> coords) {
        List<Point> out = new ArrayList<Point>(coords.size());
        BoundingBox bbox = getBoundingBox();
        Mercator proj = new Mercator();
        BoundingBox projectedBox = proj.fromWGS84(bbox);
        for (LatLng crd : coords) {
            Coord projectedCrd = Mercator.forwardMercator(crd.getLatitude(), crd.getLongitude()); //proj.fromWGS84(new Coord(crd.getLatitude(), crd.getLongitude()));
            Point p;
            if (getWidth() <= 0 || getHeight() <= 0) {
                p = new Point(-100, -100);
            } else {
                //Point p = map.getScreenCoordinate(crd);
                double projectedWidth = projectedBox.longitudeDifference();
                double projectedHeight = projectedBox.latitudeDifference();
                double xCoord = (projectedCrd.getLongitude() - projectedBox.getSouthWest().getLongitude()) / projectedWidth * getWidth();
                double yCoord = (projectedBox.getNorthEast().getLatitude() - projectedCrd.getLatitude()) / projectedHeight * getHeight();
                p = new Point((int)xCoord, (int)yCoord);
            }
            out.add(p);
        }
        return out;
    }
    
    /**
     * @deprecated For internal use only.   This is only public to allow access from the internal UWP implementation because IKVM doesn't seem to allow access to package-private methods.
     * @param mapId
     * @param zoom
     * @param lat
     * @param lon 
     */
    public static void fireMapChangeEvent(int mapId, final float zoom, final double lat, final double lon) {
        final MapContainer mc = instances.get(mapId);
        if(mc != null) {
            if(!Display.getInstance().isEdt()) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        mc.fireMapListenerEvent(zoom, lat, lon);
                    }
                });
                return;
            }
            mc.fireMapListenerEvent(zoom, lat, lon);
        }
    }
    
    /**
     * Adds a listener to user tapping on a map location, this shouldn't fire for 
     * dragging.  Note that the (x, y) coordinate of tap events are relative to the 
     * MapComponent origin, and not the screen origin.
     * 
     * @param e the tap listener
     */
    public void addTapListener(ActionListener e) {
        if(tapListener == null) {
            tapListener = new EventDispatcher();
        }
        tapListener.addListener(e);
    }
    
    /**
     * Removes the listener to user tapping on a map location, this shouldn't fire for 
     * dragging.
     * 
     * @param e the tap listener
     */
    public void removeTapListener(ActionListener e) {
        if(tapListener == null) {
            return;
        }
        tapListener.removeListener(e);
        if(!tapListener.hasListeners()) {
            tapListener = null;
        }
    }
    
    static void fireTapEventStatic(int mapId, int x, int y) {
        final MapContainer mc = instances.get(mapId);
        if(mc != null) {
            mc.fireTapEvent(x, y);
        }
    }
    
    private void fireTapEvent(int x, int y) { 
        if(tapListener != null) {
            tapListener.fireActionEvent(new ActionEvent(this, x, y));
        }
    }
    
    /**
     * Adds a listener to user long pressing on a map location, this shouldn't fire for 
     * dragging. Note the (x, y) coordinates of long press events are relative to the 
     * origin of the MapContainer and not the screen origin.
     * 
     * @param e the tap listener
     */
    public void addLongPressListener(ActionListener e) {
        if (longPressListener == null) {
            longPressListener = new EventDispatcher();
        }
        longPressListener.addListener(e);
    }

    /**
     * Removes the long press listener to user tapping on a map location, this shouldn't fire for 
     * dragging.
     * 
     * @param e the tap listener
     */
    public void removeLongPressListener(ActionListener e) {
        if (longPressListener != null) {
            longPressListener.removeListener(e);
        }
    }

    static void fireLongPressEventStatic(int mapId, int x, int y) {
        final MapContainer mc = instances.get(mapId);
        if (mc != null) {
            mc.fireLongPressEvent(x, y);
        }
    }

    private void fireLongPressEvent(int x, int y) {
        if (longPressListener != null) {
            longPressListener.fireActionEvent(new ActionEvent(this, x, y));
        }
    }
    
    void fireMapListenerEvent(float zoom, double lat, double lon) {
        float rzoom = zoom;
        // assuming always EDT
        if (dummyMapComponent != null) {
            rzoom = Math.round(zoom); //Should already be an integer in fact. But in case of... 
            dummyMapComponent.zoomTo(new Coord(lat, lon), (int) rzoom);
        }
        if(listeners != null) {
            Coord c = new Coord(lat, lon);
            for(MapListener l : listeners) {
                l.mapPositionUpdated(this, rzoom, c);
            }
        }
    }
    
    /**
     * Adds a listener to map panning/zooming Important: events are only sent when the native map is in initialized state
     * @param listener the listener callback
     */
    public void addMapListener(MapListener listener) {
        if(internalNative == null && internalLightweightCmp != null) {
            internalLightweightCmp.addMapListener(listener);
            return;
        }
        if(listeners == null) {
            listeners = new ArrayList<MapListener>();
        }
        listeners.add(listener);
    }

    /**
     * Removes the map listener callback
     * @param listener the listener
     */
    public void removeMapListener(MapListener listener) {
        if(internalNative == null && internalLightweightCmp != null) {
            internalLightweightCmp.removeMapListener(listener);
            return;
        }
        if(listeners == null) {
            return;
        }
        listeners.remove(listener);
    }

   
    
    /**
     * Object on the map
     */
    public static class MapObject {
        long mapKey;
        ActionListener callback;
        PointLayer point;
        LinesLayer lines;
        Component componentMarker; // Used only for Components added as markers
        boolean pending;
    }
}
