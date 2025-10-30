import Foundation

/// Application constants
enum Constants {
    enum Map {
        static let defaultZoom: Float = 10.0
        static let closeZoom: Float = 16.0      // Close view (street level)
        static let veryCloseZoom: Float = 18.0  // Very close view (building level)
        static let trackingZoom: Float = 17.0   // Zoom level for location tracking
        static let animationDuration: Float = 0.5
        static let blueDotSize: Float = 28.0
        
        // Zoom limits
        static let minZoomLevel: Float = 1.0
        static let maxZoomLevel: Float = 18.0
        static let zoomStep: Float = 1.0        // How many levels per zoom step
    }
    
    enum Location {
        static let defaultLatitude: Double = 39.91
        static let defaultLongitude: Double = 32.789
    }
    
    enum API {
        // Base host
        static let host = "https://bms.basarsoft.com.tr"
        // Routing endpoint
        static let routingBasePath = "/service/api/v1/Routing/BasarRouting"
        // Raster tile base (Default basemap)
        static let mapBasePath = "/service/api/v1/map/Default"
        // Vector tile base (PBF format for vector tiles)
        static let vectorTileBasePath = "/Service/api/v1/VectorMap/Pbf"
        // For tiles and services we may append appcode/accid as query params where needed
        static let baseURL = host + mapBasePath
        static let vectorTileURL = host + vectorTileBasePath
        static let appCode = Secrets.appCode
        static let accountId = Secrets.accountId
    }
}
