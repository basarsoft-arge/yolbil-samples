import Foundation
import YolbilMobileSDK

/// Model for map markers
struct MapMarker: Identifiable, Equatable {
    let id = UUID()
    let coordinate: LocationData
    let title: String?
    let subtitle: String?
    let timestamp: Date
    
    // Equatable conformance
    static func == (lhs: MapMarker, rhs: MapMarker) -> Bool {
        return lhs.id == rhs.id
    }
    
    init(coordinate: LocationData, title: String? = nil, subtitle: String? = nil) {
        self.coordinate = coordinate
        self.title = title
        self.subtitle = subtitle
        self.timestamp = Date()
    }
    
    /// Creates a marker with latitude and longitude
    init(latitude: Double, longitude: Double, title: String? = nil, subtitle: String? = nil) {
        self.coordinate = LocationData(latitude: latitude, longitude: longitude)
        self.title = title
        self.subtitle = subtitle
        self.timestamp = Date()
    }
    
    /// Converts to map coordinates using the given projection
    func mapPosition(with projection: YBProjection) -> YBMapPos? {
        return coordinate.mapPosition(with: projection)
    }
    
    /// Default marker for long press
    static func longPressMarker(at location: LocationData) -> MapMarker {
        return MapMarker(
            coordinate: location,
            title: "İşaretlenen Konum",
            subtitle: "Koordinatlar: \(String(format: "%.4f", location.latitude)), \(String(format: "%.4f", location.longitude))"
        )
    }
}

/// Marker icon types
enum MarkerIconType {
    case defaultIcon    // SDK default marker
    case pin           // Modern pin-style marker
    case circle        // Modern circle marker
    case locationDot   // GPS-style location dot
}

/// Marker style configuration
struct MarkerStyle {
    let color: MarkerColor
    let size: Float
    let iconType: MarkerIconType
    
    static let `default` = MarkerStyle(color: .red, size: 32.0, iconType: .defaultIcon)
    static let longPress = MarkerStyle(color: .modernBlue, size: 36.0, iconType: .pin)
    static let modern = MarkerStyle(color: .modernRed, size: 40.0, iconType: .pin)
    static let location = MarkerStyle(color: .modernGreen, size: 32.0, iconType: .circle)
}

/// Marker colors
enum MarkerColor {
    case red
    case blue
    case green
    case orange
    case purple
    
    // Modern gradient colors
    case modernRed
    case modernBlue
    case modernGreen
    case modernOrange
    case modernPurple
    case modernTeal
    case modernPink
    
    var rgbValue: (r: Float, g: Float, b: Float, a: Float) {
        switch self {
        // Classic colors
        case .red:
            return (1.0, 0.0, 0.0, 1.0)
        case .blue:
            return (0.0, 0.5, 1.0, 1.0)
        case .green:
            return (0.0, 0.8, 0.0, 1.0)
        case .orange:
            return (1.0, 0.6, 0.0, 1.0)
        case .purple:
            return (0.6, 0.0, 0.8, 1.0)
            
        // Modern gradient colors (iOS-inspired)
        case .modernRed:
            return (1.0, 0.231, 0.188, 1.0) // SF Red
        case .modernBlue:
            return (0.0, 0.478, 1.0, 1.0) // SF Blue
        case .modernGreen:
            return (0.204, 0.78, 0.349, 1.0) // SF Green
        case .modernOrange:
            return (1.0, 0.584, 0.0, 1.0) // SF Orange
        case .modernPurple:
            return (0.686, 0.322, 0.871, 1.0) // SF Purple
        case .modernTeal:
            return (0.353, 0.784, 0.98, 1.0) // SF Teal
        case .modernPink:
            return (1.0, 0.176, 0.333, 1.0) // SF Pink
        }
    }
}
