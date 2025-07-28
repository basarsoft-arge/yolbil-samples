import React, { useEffect, useRef } from 'react';
import { StyleSheet, View, Dimensions } from 'react-native';
import Geolocation from '@react-native-community/geolocation';
import RNFS from 'react-native-fs';
import {
  MapView,
  EPSG3857,
  MBTilesTileDataSource,
  AssetUtils,
  ZippedAssetPackage,
  CompiledStyleSet,
  MBVectorTileDecoder,
  VectorTileLayer,
  VectorTileRenderOrder,
  LocalVectorDataSource,
  GeoJSONGeometryReader,
  GeoJSONGeometryWriter,
  VectorTileSearchService,
  SearchRequest,
  PointGeometry,
  VectorLayer,
  LineStyleBuilder,
  Color,
  Line,
  LineJoinType,
  MapPos,
  MapPosVector,
  ScreenPos,
  ScreenBounds,
  MapBounds,
  HTTPTileDataSource,
  RasterTileLayer,
  BlueDotDataSource,
  LocationSource,
  LocationBuilder,
  YolbilNavigationBundleBuilder,
  MapEventListener,
  VectorTileEventListener,
} from '@basarsoft/react-native-yolbilx';

import { BASE_URL, ACCOUNT_ID, APP_CODE} from '@env';

const App = () => {
  const mapViewRef = useRef(null);
  const globals = useRef({
    proj: null,
    tileDecoder: null,
    layers: null,
    localVectorDataSource: null,
    geoJSONGeometryReader: null,
    geoJSONGeometryWriter: null,
    searchService: null,
    searchRequest: null,
    mapCenterMapPos: null,
    completedMapPosVector: null,
    incompletedMapPosVector: null,
    incompletedLine: null,
    completedLine: null,
    routingLayer: null,
    bundle: null,
    navigationResult: null,
  });

  useEffect(() => {
    const init = async () => {
      const mapView = mapViewRef.current;
      if (!mapView) return;

      await mapView.isMapReady;

      const proj = await new EPSG3857();
      globals.current.proj = proj;
      globals.current.layers = await mapView.getLayers();

      await addGoogleMaps(mapView);
      await addBlueDot(mapView);

      const searchResults = await search('a');
      console.log('search results:', searchResults);
    };

    init();
  }, []);

  const addGoogleMaps = async (mapView) => {
    const { proj } = globals.current;
    let pos = await new MapPos(32.775, 39.908);
    const center = await proj.fromWgs84(pos);
    globals.current.mapCenterMapPos = center;

    await mapView.setZoom(17, center, 0);
    const ds = await new HTTPTileDataSource(
      0,
      24,
      'https://mt0.google.com/vt/lyrs=m&hl=tr&scale=4&apistyle=s.t%3A2%7Cs.e%3Al%7Cp.v%3Aoff&x={x}&y={y}&z={zoom}'
    );
    const rasterLayer = await new RasterTileLayer(ds);
    await globals.current.layers.add(rasterLayer);
  };

  const addBlueDot = async (mapView) => {
    Geolocation.getCurrentPosition(async position => {
      const { latitude, longitude } = position.coords;
      const { proj, layers } = globals.current;

      const startPos = await proj.fromWgs84(
        await new MapPos(longitude, latitude)
      );

      const locationSource = await new LocationSource();
      const locationBuilder = await new LocationBuilder();
      const blueDotDataSource = await new BlueDotDataSource(
        proj,
        locationSource
      );
      const vectorLayer = await new VectorLayer(blueDotDataSource);
      await layers.add(vectorLayer);

      await locationBuilder.setCoordinate(startPos);
      await locationBuilder.setHorizontalAccuracy(20);
      await locationBuilder.setDirection(290);
      await locationBuilder.setDirectionAccuracy(1);
      await locationSource.updateLocation(await locationBuilder.build());
      await mapView.setFocusPos(startPos, 0);
      await mapView.setZoom(17, 0);

      const start = await new MapPos(longitude, latitude);
      const end = await new MapPos(32.789, 39.91);
      await createNavigation(mapView, start, end);
      await startNavigation(mapView);
    });
  };

  const search = async (text) => {
    const {
      searchRequest,
      searchService,
      geoJSONGeometryWriter,
      mapCenterMapPos,
    } = globals.current;

    if (!searchRequest || !searchService || !geoJSONGeometryWriter) return [];

    await searchRequest.setGeometry(await new PointGeometry(mapCenterMapPos));
    await searchRequest.setFilterExpression(
      `REGEXP_ILIKE(name,'(.*)${text}(.*)') AND categories IS NOT NULL`
    );
    const features = await searchService.findFeatures(searchRequest);
    const featuresJson = JSON.parse(await geoJSONGeometryWriter.writeFeatureCollection(features));

    return featuresJson.features.map(f => ({
      id: f.properties.id,
      name: f.properties.name,
      floor: f.properties.floor,
      categories: JSON.parse(f.properties.categories),
    }));
  };

  const createNavigation = async (mapView, start, end) => {
    const { proj, layers } = globals.current;

    const locationSource = await new LocationSource();
    const builder = await new YolbilNavigationBundleBuilder(
        BASE_URL,
        ACCOUNT_ID,
        APP_CODE,
        locationSource
      );

    const bundle = await builder.build();
    await layers.addAll(await bundle.getLayers());

    const navigationResult = await bundle.startNavigation(start, end);

    if (!navigationResult) {
      console.warn('Navigation result is null.');
      return;
    }

    globals.current.bundle = bundle;
    globals.current.navigationResult = navigationResult;
  };

  const startNavigation = async (mapView) => {
    const { bundle, navigationResult } = globals.current;
    if (bundle && navigationResult) {
      await bundle.beginNavigation(navigationResult);
      await mapView.setDeviceOrientationFocused(true);
    }
  };

  return (
    <View style={styles.container}>
      <MapView
        ref={mapViewRef}
        license="inavi"
        style={styles.mapview}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  mapview: {
    flex: 1,
  },
});

export default App;
