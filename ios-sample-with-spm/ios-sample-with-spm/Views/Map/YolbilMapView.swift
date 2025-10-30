import SwiftUI
import YolbilMobileSDK
import UIKit

/// UIKit map view wrapped for SwiftUI
struct YolbilMapView: UIViewRepresentable {
    @ObservedObject var viewModel: MapViewModel
    
    func makeUIView(context: Context) -> YBMapView {
        guard let mapView = viewModel.createMapView() else {
            // Return a placeholder view if map creation fails
            return YBMapView() ?? UIView() as! YBMapView
        }
        
        // Add long-press gesture
        let longPressGesture = UILongPressGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleLongPress(_:))
        )
        longPressGesture.minimumPressDuration = 0.5
        mapView.addGestureRecognizer(longPressGesture)
        
        // When the user pans the map, cancel follow focus
        let panGesture = UIPanGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handlePan(_:))
        )
        panGesture.cancelsTouchesInView = false
        mapView.addGestureRecognizer(panGesture)

        // Tap gesture: In Developer Mode, set tapped point as mock location
        let tapGesture = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleTap(_:)))
        tapGesture.cancelsTouchesInView = false
        mapView.addGestureRecognizer(tapGesture)
        
        // Provide map view reference to the Coordinator
        context.coordinator.mapView = mapView
        
        return mapView
    }
    
    func updateUIView(_ uiView: YBMapView, context: Context) {
        // Handle any updates if needed
        // The view model handles most updates automatically
        context.coordinator.mapView = uiView
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(viewModel: viewModel)
    }
    
    @MainActor class Coordinator {
        let viewModel: MapViewModel
        weak var mapView: YBMapView?
        
        init(viewModel: MapViewModel) {
            self.viewModel = viewModel
        }
        
        @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
            guard gesture.state == .began,
                  let mapView = mapView else { return }
            
            // Dismiss keyboard and search UI if visible before placing marker
            DispatchQueue.main.async { [weak self] in
                self?.viewModel.dismissSearchUIAndKeyboard()
            }

            // Get gesture location
            let touchPoint = gesture.location(in: mapView)
            
            // Get device scale factor (for Retina displays)
            let scale = UIScreen.main.scale
            print("Touch point: \(touchPoint), Scale: \(scale)")
            print("MapView bounds: \(mapView.bounds)")
            print("MapView frame: \(mapView.frame)")
            
            // Compute correct coordinates using the scale factor
            let scaledPoint = CGPoint(x: touchPoint.x * scale, y: touchPoint.y * scale)
            
            print("Coordinate conversion:")
            print("- Touch point: \(touchPoint)")
            print("- Scale factor: \(scale)")
            print("- Scaled point: \(scaledPoint)")
            
            // Use coordinates multiplied by the scale factor
            let screenPos = YBScreenPos(x: Float(scaledPoint.x), y: Float(scaledPoint.y))
            guard let mapPos = mapView.screen(toMap: screenPos) else {
                print("Failed to convert screen to map coordinates")
                return
            }
            
            print("Map position: x=\(mapPos.getX()), y=\(mapPos.getY())")
            
            // Convert coordinates to WGS84 (real lat/lon)
            let options = mapView.getOptions()
            guard let projection = options?.getBaseProjection() else {
                print("Failed to get base projection")
                return
            }
            
            let wgs84Pos = projection.toWgs84(mapPos)
            
            // Build coordinates (WGS84 coordinate system)
            let coordinate = LocationData(
                latitude: Double(wgs84Pos?.getY() ?? 0),
                longitude: Double(wgs84Pos?.getX() ?? 0)
            )
            
            print("Final coordinates: lat=\(coordinate.latitude), lon=\(coordinate.longitude)")
            
            // Notify ViewModel to add marker (run on main actor)
            DispatchQueue.main.async { [weak self] in
                self?.viewModel.addMarkerAtLocation(coordinate)
            }
            
            print("Long press detected at: \(coordinate.latitude), \(coordinate.longitude)")
        }

        @objc func handlePan(_ gesture: UIPanGestureRecognizer) {
            // When drag begins, only disable follow mode (do not stop GPS)
            if gesture.state == .began {
                DispatchQueue.main.async { [weak self] in
                    self?.viewModel.isFollowingUser = false
                }
            }
        }

        @objc func handleTap(_ gesture: UITapGestureRecognizer) {
            guard let mapView = mapView else { return }
            guard viewModel.developerModeEnabled else { return }

            let point = gesture.location(in: mapView)
            let scale = UIScreen.main.scale
            let scaledPoint = CGPoint(x: point.x * scale, y: point.y * scale)
            let screenPos = YBScreenPos(x: Float(scaledPoint.x), y: Float(scaledPoint.y))
            guard let mapPos = mapView.screen(toMap: screenPos) else { return }
            DispatchQueue.main.async { [weak self] in
                self?.viewModel.applyDeveloperMockLocation(mapPos: mapPos)
                // Ensure tracking is on so the blue dot updates from mock source
                if self?.viewModel.isLocationEnabled == false {
                    self?.viewModel.startLocationTracking()
                }
                // When mocking, also inject a heading pointing to tap from screen center (rough visual)
                if let mv = self?.mapView, let proj = mv.getOptions()?.getBaseProjection(), let wgs = proj.toWgs84(mapPos) {
                    let tapLat = Double(wgs.getY())
                    let tapLon = Double(wgs.getX())
                    self?.viewModel.currentLocation = LocationData(latitude: tapLat, longitude: tapLon, headingDegrees: nil, speedMps: nil)
                }
            }
        }
    }
}
