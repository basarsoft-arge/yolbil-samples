import Foundation

/// Application-specific error types
enum AppError: LocalizedError {
    case mapInitializationFailed
    case locationServiceUnavailable
    case invalidCoordinates
    case networkError(String)
    
    var errorDescription: String? {
        switch self {
        case .mapInitializationFailed:
            return "Map view could not be initialized"
        case .locationServiceUnavailable:
            return "Location services are not available"
        case .invalidCoordinates:
            return "Invalid coordinates provided"
        case .networkError(let message):
            return "Network error: \(message)"
        }
    }
}
