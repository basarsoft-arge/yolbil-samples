import Foundation
import Combine
import UIKit
import YolbilMobileSDK

/// Service responsible for map configuration and layer management
class MapService: ObservableObject {
    private let configuration: MapConfiguration
    
    init(configuration: MapConfiguration = .default) {
        self.configuration = configuration
    }
    
    /// Creates and configures a map view with default settings
    func createMapView() throws -> YBMapView {
        guard let mapView = YBMapView() else {
            throw AppError.mapInitializationFailed
        }
        
        configureMapView(mapView)
        return mapView
    }
    
    /// Configures the map view with base projection and layers
    private func configureMapView(_ mapView: YBMapView) {
        mapView.getOptions()?.setBaseProjection(YBEPSG4326())
        addTileLayer(to: mapView)
    }
    
    /// Adds the tile layer to the map view (now using vector tiles)
    private func addTileLayer(to mapView: YBMapView) {
        // Try to create vector tile layer first
        if let vectorTileLayer = getVectorTileLayer(theme: getSystemTheme()) {
            mapView.getLayers()?.add(vectorTileLayer)
            print("Vector tile layer başarıyla eklendi")
        } else {
            // Fallback to raster tiles if vector tiles fail
            print("Vector tile layer oluşturulamadı, raster tile layer kullanılıyor")
            let tileDataSource = YBHTTPTileDataSource(
                minZoom: configuration.minZoom,
                maxZoom: configuration.maxZoom,
                baseURL: configuration.baseURL
            )

            let subdomains = YBStringVector()
            configuration.subdomains.forEach { subdomain in
                subdomains?.add(subdomain)
            }
            tileDataSource?.setSubdomains(subdomains)

            let tileLayer = YBRasterTileLayer(dataSource: tileDataSource)
            mapView.getLayers()?.add(tileLayer)
        }
    }
    
    /// Creates a blue dot layer for location display
    func createBlueDotLayer(
        locationSource: GPSLocationSource,
        initialLocation: LocationData
    ) -> YBVectorLayer? {
        guard let projection = YBEPSG4326() else {
            print("Failed to create EPSG4326 projection")
            return nil
        }
        
        let blueDotDataSource = YBBlueDotDataSource(
            projection: projection,
            locationSource: locationSource
        )
        
        blueDotDataSource?.init()
        // Custom modern blue dot (ring + inner dot), with slight animation for smooth updates
        if let dotBitmap = makeBlueDotBitmap(size: CGFloat(configuration.blueDotSize)) {
            blueDotDataSource?.setCustomBitmap(dotBitmap, size: configuration.blueDotSize)
            blueDotDataSource?.setAnimationDuration(0.2)
        } else {
            blueDotDataSource?.useDefaultCenterBitmapMarker(configuration.blueDotSize)
        }
        
        // Convert LocationData to map coordinates
        guard let mapPos = initialLocation.mapPosition(with: projection) else {
            print("Failed to convert initial location coordinates")
            return YBVectorLayer(dataSource: blueDotDataSource)
        }
        
        let locationBuilder = YBLocationBuilder()
        locationBuilder?.setCoordinate(mapPos)
        
        if let location = locationBuilder?.build() {
            blueDotDataSource?.updateBlueDot(location)
        }
        
        return YBVectorLayer(dataSource: blueDotDataSource)
    }

    // MARK: - Blue dot rendering
    private func makeBlueDotBitmap(size: CGFloat) -> YBBitmap? {
        guard size > 0 else { return nil }
        _ = UIScreen.main.scale
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: size, height: size))
        let img = renderer.image { ctx in
            let ctx = ctx.cgContext
            ctx.scaleBy(x: 1, y: 1)

            let center = CGPoint(x: size / 2, y: size / 2)
            let outerRadius = size / 2
            let ringWidth = max(2, size * 0.10)
            let innerRadius = max(2, size * 0.28)

            // Outer halo (soft radial gradient for 3D effect)
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [UIColor(red: 0.08, green: 0.48, blue: 1.0, alpha: 0.25).cgColor, UIColor.clear.cgColor] as CFArray, locations: [0.0, 1.0]) {
                ctx.drawRadialGradient(gradient, startCenter: center, startRadius: innerRadius, endCenter: center, endRadius: outerRadius, options: .drawsBeforeStartLocation)
            } else {
                ctx.setFillColor(UIColor(red: 0.08, green: 0.48, blue: 1.0, alpha: 0.18).cgColor)
                ctx.addEllipse(in: CGRect(x: center.x - outerRadius, y: center.y - outerRadius, width: outerRadius * 2, height: outerRadius * 2))
                ctx.fillPath()
            }

            // Ring stroke
            ctx.setStrokeColor(UIColor(red: 0.08, green: 0.48, blue: 1.0, alpha: 0.95).cgColor)
            ctx.setLineWidth(ringWidth)
            let ringRect = CGRect(x: center.x - (outerRadius - ringWidth / 2), y: center.y - (outerRadius - ringWidth / 2), width: (outerRadius - ringWidth / 2) * 2, height: (outerRadius - ringWidth / 2) * 2)
            ctx.addEllipse(in: ringRect)
            ctx.strokePath()

            // Soft ground shadow (3D depth)
            ctx.saveGState()
            let shadowRect = CGRect(x: center.x - innerRadius, y: center.y + innerRadius * 0.6, width: innerRadius * 2, height: innerRadius * 0.7)
            ctx.setFillColor(UIColor.black.withAlphaComponent(0.22).cgColor)
            ctx.addEllipse(in: shadowRect)
            ctx.fillPath()
            ctx.restoreGState()

            // Inner dot (white with blue edge)
            ctx.setFillColor(UIColor.white.cgColor)
            ctx.addEllipse(in: CGRect(x: center.x - innerRadius, y: center.y - innerRadius, width: innerRadius * 2, height: innerRadius * 2))
            ctx.fillPath()

            ctx.setStrokeColor(UIColor(red: 0.08, green: 0.48, blue: 1.0, alpha: 1.0).cgColor)
            ctx.setLineWidth(max(1, size * 0.06))
            ctx.addEllipse(in: CGRect(x: center.x - innerRadius, y: center.y - innerRadius, width: innerRadius * 2, height: innerRadius * 2))
            ctx.strokePath()

            // Glossy highlight on top-left (3D shine)
            ctx.setFillColor(UIColor.white.withAlphaComponent(0.15).cgColor)
            let highlightRect = CGRect(x: center.x - innerRadius * 0.8, y: center.y - innerRadius * 1.15, width: innerRadius * 1.6, height: innerRadius * 1.1)
            ctx.addEllipse(in: highlightRect)
            ctx.fillPath()

            // Direction arrow (points up, BlueDot will rotate it via heading)
            let arrowHeight = max(3, size * 0.24)
            let arrowWidth = max(3, size * 0.18)
            let tip = CGPoint(x: center.x, y: center.y - innerRadius + max(1, size * 0.04))
            let left = CGPoint(x: tip.x - arrowWidth / 2, y: tip.y + arrowHeight)
            let right = CGPoint(x: tip.x + arrowWidth / 2, y: tip.y + arrowHeight)
            ctx.setFillColor(UIColor(red: 0.08, green: 0.48, blue: 1.0, alpha: 1.0).cgColor)
            ctx.beginPath()
            ctx.move(to: tip)
            ctx.addLine(to: left)
            ctx.addLine(to: right)
            ctx.closePath()
            ctx.fillPath()
        }

        return YBBitmapUtils.createBitmap(from: img)
    }
    
    /// Sets the map focus to a specific location
    func setMapFocus(
        _ mapView: YBMapView,
        to location: LocationData,
        zoom: Float? = nil,
        animated: Bool = true
    ) {
        let zoomLevel = zoom ?? configuration.initialZoom
        let duration: Float = animated ? 0.5 : 0
        
        // Get map projection and convert coordinates
        guard let options = mapView.getOptions(),
              let projection = options.getBaseProjection(),
              let mapPos = location.mapPosition(with: projection) else {
            print("Failed to convert location coordinates for map focus")
            return
        }
        
        mapView.setZoom(zoomLevel, durationSeconds: duration)
        mapView.setFocus(mapPos, durationSeconds: duration)
    }
    
    /// Gets the current zoom level of the map
    func getCurrentZoom(_ mapView: YBMapView) -> Float {
        return mapView.getZoom()
    }
    
    /// Zooms in by one level
    func zoomIn(_ mapView: YBMapView, animated: Bool = true) {
        let currentZoom = getCurrentZoom(mapView)
        let newZoom = min(currentZoom + 1, Float(configuration.maxZoom))
        let duration: Float = animated ? Constants.Map.animationDuration : 0
        
        mapView.setZoom(newZoom, durationSeconds: duration)
    }
    
    /// Zooms out by one level
    func zoomOut(_ mapView: YBMapView, animated: Bool = true) {
        let currentZoom = getCurrentZoom(mapView)
        let newZoom = max(currentZoom - 1, Float(configuration.minZoom))
        let duration: Float = animated ? Constants.Map.animationDuration : 0
        
        mapView.setZoom(newZoom, durationSeconds: duration)
    }
    
    /// Sets a specific zoom level
    func setZoom(_ mapView: YBMapView, to zoomLevel: Float, animated: Bool = true) {
        let clampedZoom = max(Float(configuration.minZoom), min(Float(configuration.maxZoom), zoomLevel))
        let duration: Float = animated ? Constants.Map.animationDuration : 0

        mapView.setZoom(clampedZoom, durationSeconds: duration)
    }

    /// Creates a vector tile layer with the specified theme
    func getVectorTileLayer(theme: String = "light") -> YBVectorTileLayer? {
        print("Vektör harita yükleniyor... Tema: \(theme)")

        // Load style file
        let styleFileName = "transport_style_final_package_latest_\(theme).zip"

        // Load asset
        guard let styleAsset = YBAssetUtils.loadAsset(styleFileName) else {
            print("Stil dosyası bulunamadı: \(styleFileName)")
            return nil
        }

        // Create HTTP data source and tile decoder
        let httpTileDataSource = YBHTTPTileDataSource(
            minZoom: 0,
            maxZoom: 15,
            baseURL: configuration.vectorTileURL
        )

        let subdomains = YBStringVector()
        configuration.subdomains.forEach { subdomain in
            subdomains?.add(subdomain)
        }
        httpTileDataSource?.setSubdomains(subdomains)

        do {
            // Create asset package
            let assetPackage = YBZippedAssetPackage(zip: styleAsset)

            // Create compiled style set
            let compiledStyleSet = YBCompiledStyleSet(assetPackage: assetPackage, styleName: "transport_style")

            // Create tile decoder
            let tileDecoder = YBMBVectorTileDecoder(compiledStyleSet: compiledStyleSet)
            tileDecoder?.setStyleParameter("selectedTheme", value: theme)

            // Create VectorTileLayer
            guard let vectorTileLayer = YBVectorTileLayer(dataSource: httpTileDataSource, decoder: tileDecoder) else {
                print("Vektör katmanı oluşturulamadı")
                return nil
            }
            return vectorTileLayer

            // Layer settings
            //vectorTileLayer.setLabelRenderOrder(YBVectorTileRenderOrder.YB_VECTOR_TILE_RENDER_ORDER_LAST) // YB_VECTOR_TILE_RENDER_ORDER_LAST
            //vectorTileLayer.setBuildingRenderOrder(0) // YB_VECTOR_TILE_RENDER_ORDER_LAYER


        } catch {
            print("Vektör katmanı oluşturulurken hata: \(error.localizedDescription)")
            return nil
        }
    }

    /// Determines the system theme (light/dark)
    func getSystemTheme() -> String {
        if #available(iOS 13.0, *) {
            return UITraitCollection.current.userInterfaceStyle == .dark ? "dark" : "light"
        } else {
            return "light"
        }
    }

    /// Reloads vector tile layer with new theme
    func reloadVectorTileLayer(to mapView: YBMapView, theme: String) {
        guard let layers = mapView.getLayers() else { return }



        // Add new vector tile layer with updated theme
        if let newVectorTileLayer = getVectorTileLayer(theme: theme) {
            layers.add(newVectorTileLayer)
            print("Vector tile layer reloaded with theme change: \(theme)")
        } else {
            print("Failed to create vector tile layer with new theme: \(theme)")
        }
    }

    /// Changes map theme (light/dark/system)
    func changeMapTheme(to mapView: YBMapView, theme: String) {
        switch theme {
        case "dark":
            reloadVectorTileLayer(to: mapView, theme: "dark")
        case "light":
            reloadVectorTileLayer(to: mapView, theme: "light")
        case "system":
            let systemTheme = getSystemTheme()
            reloadVectorTileLayer(to: mapView, theme: systemTheme)
        default:
            print("Invalid theme: \(theme)")
        }
    }
}
