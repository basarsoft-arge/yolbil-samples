import Foundation
import YolbilMobileSDK
import UIKit

/// Service responsible for managing map markers
class MarkerService: ObservableObject {
    @Published var markers: [MapMarker] = []
    @Published var singleMarkerMode: Bool = true  // @Published for UI updates
    
    private var markerLayer: YBVectorLayer?
    private var markerDataSource: YBLocalVectorDataSource?
    private var projection: YBProjection?
    
    init() {
        setupMarkerLayer()
    }
    
    private func setupMarkerLayer() {
        // Use EPSG4326 projection (WGS84)
        self.projection = YBEPSG4326()
        
        guard let projection = self.projection,
              let dataSource = YBLocalVectorDataSource(projection: projection) else {
            print("Failed to create marker data source")
            return
        }
        
        self.markerDataSource = dataSource
        self.markerLayer = YBVectorLayer(dataSource: dataSource)
    }
    
    /// Adds the marker layer to the map
    func addMarkerLayerToMap(_ mapView: YBMapView) {
        guard let markerLayer = markerLayer else { return }
        mapView.getLayers()?.add(markerLayer)
        print("Marker layer added to map")
    }
    
    /// Adds a new marker to the map
    func addMarker(_ marker: MapMarker, style: MarkerStyle = .longPress) {
        guard let dataSource = markerDataSource,
              let projection = projection else { return }
        
        // In single marker mode, clear previous markers
        if singleMarkerMode {
            markers.removeAll()
            dataSource.clear()
            print("Previous markers cleared (single marker mode)")
        }
        
        // Append marker to the list
        markers.append(marker)
        
        // Create modern YolbilMobileSDK marker style
        guard let markerStyle = createMarkerStyle(for: style) else {
            print("Failed to create marker style")
            return
        }
        
        // Convert coordinates using the correct projection
        guard let mapPos = marker.coordinate.mapPosition(with: projection) else {
            print("Failed to convert coordinates for marker")
            return
        }
        
        // Create marker and add to the data source
        let markerObject = YBMarker(pos: mapPos, style: markerStyle)
        dataSource.add(markerObject)
        print("Marker added at: \(marker.coordinate.latitude), \(marker.coordinate.longitude)")
    }
    
    /// Removes a specific marker
    func removeMarker(_ marker: MapMarker) {
        guard let dataSource = markerDataSource else { return }
        
        // Remove marker from the list
        markers.removeAll { $0.id == marker.id }
        
        // Clear data source and re-add remaining markers
        dataSource.clear()
        guard let projection = projection else { return }
        
        for remainingMarker in markers {
            let style = MarkerStyle.longPress
            
            if let markerStyle = createMarkerStyle(for: style),
               let mapPos = remainingMarker.coordinate.mapPosition(with: projection) {
                let markerObject = YBMarker(pos: mapPos, style: markerStyle)
                dataSource.add(markerObject)
            }
        }
        
        print("Marker removed")
    }
    
    /// Removes all markers
    func removeAllMarkers() {
        guard let dataSource = markerDataSource else { return }
        
        markers.removeAll()
        dataSource.clear()
        print("All markers removed")
    }
    
    /// Finds the nearest marker to a given coordinate
    func nearestMarker(to coordinate: LocationData, maxDistance: Double = 100.0) -> MapMarker? {
        guard !markers.isEmpty else { return nil }
        
        var nearestMarker: MapMarker?
        var nearestDistance = Double.infinity
        
        for marker in markers {
            let distance = calculateDistance(
                from: coordinate,
                to: marker.coordinate
            )
            
            if distance < nearestDistance && distance <= maxDistance {
                nearestDistance = distance
                nearestMarker = marker
            }
        }
        
        return nearestMarker
    }
    
    /// Calculates distance between two coordinates (simplified)
    private func calculateDistance(from: LocationData, to: LocationData) -> Double {
        let latDiff = from.latitude - to.latitude
        let lonDiff = from.longitude - to.longitude
        return sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111000 // Approximate meters
    }
    
    /// Gets marker layer for map integration
    func getMarkerLayer() -> YBVectorLayer? {
        return markerLayer
    }
    
    /// Toggles single marker mode
    func toggleSingleMarkerMode() {
        singleMarkerMode.toggle()
        print("Single marker mode: \(singleMarkerMode ? "ON" : "OFF")")
    }
    
    /// Sets single marker mode
    func setSingleMarkerMode(_ enabled: Bool) {
        singleMarkerMode = enabled
        print("Single marker mode set to: \(enabled ? "ON" : "OFF")")
    }
    
    // MARK: - Private Helper Methods
    
    /// Creates a modern marker style with custom icon or color
    private func createMarkerStyle(for style: MarkerStyle) -> YBMarkerStyle? {
        let markerStyleBuilder = YBMarkerStyleBuilder()
        markerStyleBuilder?.setSize(style.size)
        
        // Set anchor points based on icon type (SDK uses: -1=left/bottom, 0=center, 1=right/top)
        switch style.iconType {
        case .pin:
            markerStyleBuilder?.setAnchorPointX(0.0) // Center horizontally
            markerStyleBuilder?.setAnchorPointY(-1.0) // Bottom anchor (pin style)
        case .circle, .locationDot:
            markerStyleBuilder?.setAnchorPointX(0.0) // Center horizontally
            markerStyleBuilder?.setAnchorPointY(0.0) // Center vertically
        case .defaultIcon:
            markerStyleBuilder?.setAnchorPointX(0.0) // Center horizontally
            markerStyleBuilder?.setAnchorPointY(-1.0) // Bottom anchor (default)
        }
        
        // Set click area
        markerStyleBuilder?.setClickSize(style.size * 1.5) // Larger click area
        
        // Set scaling mode to keep marker size constant during zoom
        if let constScreenSize = YBBillboardScaling(rawValue: 2) { // CONST_SCREEN_SIZE = 2
            markerStyleBuilder?.setScalingMode(constScreenSize)
        }
        
        // Create custom icon or use default color
        if style.iconType != .defaultIcon {
            // Create custom bitmap icon
            let iconImage = createCustomIcon(for: style)
            if let image = iconImage,
               let bitmap = YBBitmapUtils.createBitmap(from: image) {
                markerStyleBuilder?.setBitmap(bitmap)
                print("Custom marker icon created for type: \(style.iconType)")
            } else {
                print("Failed to create custom icon, falling back to color")
                fallbackToColorMarker(markerStyleBuilder, style: style)
            }
        } else {
            // Use SDK default with custom color
            fallbackToColorMarker(markerStyleBuilder, style: style)
        }
        
        return markerStyleBuilder?.buildStyle()
    }
    
    /// Creates custom icon UIImage based on style
    private func createCustomIcon(for style: MarkerStyle) -> UIImage? {
        let color = style.color.uiColor
        let size = CGFloat(style.size)
        
        switch style.iconType {
        case .pin:
            return ModernMarkerIcon.createPinIcon(color: color, size: size)
        case .circle:
            return ModernMarkerIcon.createCircleIcon(color: color, size: size)
        case .locationDot:
            return ModernMarkerIcon.createLocationDotIcon(color: color, size: size)
        case .defaultIcon:
            return nil
        }
    }
    
    /// Fallback to SDK default marker with color
    private func fallbackToColorMarker(_ builder: YBMarkerStyleBuilder?, style: MarkerStyle) {
        let color = style.color.rgbValue
        let markerColor = YBColor(r: UInt8(color.r * 255), g: UInt8(color.g * 255), b: UInt8(color.b * 255), a: UInt8(color.a * 255))
        builder?.setColor(markerColor)
    }
}
