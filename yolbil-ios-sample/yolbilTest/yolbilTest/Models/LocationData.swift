import Foundation
import YolbilMobileSDK

/// Model for location data
struct LocationData: Equatable {
    let latitude: Double
    let longitude: Double
    let headingDegrees: Double?   // 0..360, 0=north
    let speedMps: Double?         // meters per second
    let timestamp: Date
    
    /// Creates a YBMapPos in WGS84 coordinates (needs projection conversion for map use)
    var wgs84Position: YBMapPos {
        return YBMapPos(x: longitude, y: latitude)
    }
    
    /// Converts to map coordinates using the given projection
    func mapPosition(with projection: YBProjection) -> YBMapPos? {
        let wgs84Pos = YBMapPos(x: longitude, y: latitude)
        return projection.fromWgs84(wgs84Pos)
    }
    
    init(latitude: Double, longitude: Double, headingDegrees: Double? = nil, speedMps: Double? = nil, timestamp: Date = Date()) {
        self.latitude = latitude
        self.longitude = longitude
        self.headingDegrees = headingDegrees
        self.speedMps = speedMps
        self.timestamp = timestamp
    }
    
    // Default location (Ankara, Turkey)
    static let defaultLocation = LocationData(
        latitude: Constants.Location.defaultLatitude,
        longitude: Constants.Location.defaultLongitude
    )
}
