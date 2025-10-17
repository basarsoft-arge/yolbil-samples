import Foundation
import YolbilMobileSDK
import INVSensorManager
import CoreLocation

/// Service responsible for handling location updates and GPS functionality
class LocationService: NSObject, ObservableObject {
    @Published var currentLocation: LocationData?
    @Published var isLocationEnabled: Bool = false
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    
    private(set) var locationSource: GPSLocationSource?
    private var coreLocationManager: CLLocationManager?
    private var locationUpdateTimer: Timer?
    private var allowCoreLocationUpdates: Bool = true
    private var allowSDKGPSUpdates: Bool = true
    
    override init() {
        super.init()
        setupLocationService()
        setupCoreLocation()
    }
    
    private func setupLocationService() {
        guard let locationSource = GPSLocationSource() else {
            print("Failed to initialize GPS location source")
            return
        }
        
        self.locationSource = locationSource
    }
    
    private func setupCoreLocation() {
        coreLocationManager = CLLocationManager()
        coreLocationManager?.delegate = self
        coreLocationManager?.desiredAccuracy = kCLLocationAccuracyBest
        
        // Request permission
        requestLocationPermission()
    }
    
    private func requestLocationPermission() {
        guard let manager = coreLocationManager else { return }
        
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .denied, .restricted:
            print("Location access denied")
        case .authorizedWhenInUse, .authorizedAlways:
            print("Location access granted")
        @unknown default:
            print("Unknown location authorization status")
        }
    }
    
    func startLocationUpdates() {
        guard let manager = coreLocationManager else { return }
        
        // Start Core Location
        if allowCoreLocationUpdates {
            if manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways {
                manager.startUpdatingLocation()
            }
        }
        
        // Start Yolbil GPS source (optional)
        if allowSDKGPSUpdates {
            locationSource?.startLocationUpdates()
        }
        isLocationEnabled = true
        
        print("Location tracking started")
    }
    
    func stopLocationUpdates() {
        coreLocationManager?.stopUpdatingLocation()
        locationSource?.stopLocationUpdates()
        locationUpdateTimer?.invalidate()
        isLocationEnabled = false
        
        print("Location tracking stopped")
    }
    
    /// Manually update location (for testing purposes)
    func updateLocation(latitude: Double, longitude: Double) {
        let locationData = LocationData(latitude: latitude, longitude: longitude)
        DispatchQueue.main.async { [weak self] in
            self?.currentLocation = locationData
            print("Location updated manually: \(latitude), \(longitude)")
        }
    }
    
    deinit {
        stopLocationUpdates()
    }
}

// MARK: - CLLocationManagerDelegate
extension LocationService: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        let validHeading: Double? = {
            let c = location.course
            if c.isNaN { return nil }
            return (c >= 0 && c <= 360) ? Double(c) : nil
        }()
        let validSpeed: Double? = {
            let s = location.speed
            if s.isNaN { return nil }
            return (s >= 0) ? Double(s) : nil
        }()
        let locationData = LocationData(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            headingDegrees: validHeading,
            speedMps: validSpeed
        )
        
        DispatchQueue.main.async { [weak self] in
            self?.currentLocation = locationData
            print("Real location updated: \(location.coordinate.latitude), \(location.coordinate.longitude)")
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location manager failed with error: \(error.localizedDescription)")
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        DispatchQueue.main.async { [weak self] in
            self?.authorizationStatus = status
            print("Location authorization status changed: \(status.rawValue)")
            
            switch status {
            case .authorizedWhenInUse, .authorizedAlways:
                if self?.isLocationEnabled == true, self?.allowCoreLocationUpdates == true {
                    manager.startUpdatingLocation()
                }
            case .denied, .restricted:
                self?.stopLocationUpdates()
            default:
                break
            }
        }
    }
}

// MARK: - Controls for CoreLocation updates (used by Developer Mode)
extension LocationService {
    /// Enables or disables CoreLocation location updates without affecting Yolbil GPS source.
    func setCoreLocationActive(_ active: Bool) {
        allowCoreLocationUpdates = active
        guard let manager = coreLocationManager else { return }
        if active {
            if isLocationEnabled && (manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways) {
                manager.startUpdatingLocation()
            }
        } else {
            manager.stopUpdatingLocation()
        }
    }

    /// Enables or disables the SDK's GPSLocationSource hardware updates. Mock updates still work when disabled.
    func setSDKGPSActive(_ active: Bool) {
        allowSDKGPSUpdates = active
        if active {
            if isLocationEnabled {
                locationSource?.startLocationUpdates()
            }
        } else {
            locationSource?.stopLocationUpdates()
        }
    }
}
