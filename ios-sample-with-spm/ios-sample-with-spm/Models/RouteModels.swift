import Foundation
import YolbilMobileSDK

// MARK: - Route Request Models
struct RouteRequest {
    let startLocation: LocationData
    let endLocation: LocationData
    let routeType: RouteType
    
    enum RouteType: String, CaseIterable {
        case car = "car"
        case truck = "truck"
        case pedestrian = "pedestrian"
        
        var displayName: String {
            switch self {
            case .car:
                return "Araç"
            case .truck:
                return "Kamyon"
            case .pedestrian:
                return "Yaya"
            }
        }
        
        var icon: String {
            switch self {
            case .car:
                return "car.fill"
            case .truck:
                if #available(iOS 17.0, *) {
                    return "truck.box.fill"
                } else {
                    return "bus.fill"
                }
            case .pedestrian:
                return "figure.walk"
            }
        }
        
        var color: String {
            switch self {
            case .car:
                return "blue"
            case .truck:
                return "purple"
            case .pedestrian:
                return "orange"
            }
        }
    }
}

// MARK: - Route Response Models
struct RouteInfo: Equatable {
    let distance: Double // meters
    let duration: Double // seconds
    let routeType: RouteRequest.RouteType
    let routeGeometry: [LocationData] // Route points
    let instructions: [RouteInstruction]
    
    var distanceText: String {
        if distance >= 1000 {
            return String(format: "%.1f km", distance / 1000)
        } else {
            return String(format: "%.0f m", distance)
        }
    }
    
    var durationText: String {
        let hours = Int(duration) / 3600
        let minutes = (Int(duration) % 3600) / 60
        
        if hours > 0 {
            return "\(hours)sa \(minutes)dk"
        } else {
            return "\(minutes)dk"
        }
    }
}

struct RouteInstruction: Equatable {
    let text: String
    let distance: Double
    let duration: Double
    let location: LocationData
    let icon: String
}

// MARK: - Route State
enum RouteState: Equatable {
    case idle
    case calculating
    case calculated(RouteInfo)
    case error(String)
    
    // Equatable conformance for associated values
    static func == (lhs: RouteState, rhs: RouteState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.calculating, .calculating):
            return true
        case (.calculated(let lhsRoute), .calculated(let rhsRoute)):
            return lhsRoute.distance == rhsRoute.distance && 
                   lhsRoute.duration == rhsRoute.duration &&
                   lhsRoute.routeType == rhsRoute.routeType
        case (.error(let lhsMessage), .error(let rhsMessage)):
            return lhsMessage == rhsMessage
        default:
            return false
        }
    }
}
