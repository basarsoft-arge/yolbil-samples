import Foundation
import YolbilMobileSDK

/// Configuration model for map settings
struct MapConfiguration {
    let baseURL: String
    let vectorTileURL: String
    let subdomains: [String]
    let minZoom: Int32
    let maxZoom: Int32
    let initialZoom: Float
    let blueDotSize: Float

    static let `default` = MapConfiguration(
        baseURL: "\(Constants.API.host)\(Constants.API.mapBasePath)?appcode=\(Constants.API.appCode)&accid=\(Constants.API.accountId)&&x={x}&y={y}&z={zoom}",
        vectorTileURL: "\(Constants.API.vectorTileURL)?appcode=\(Constants.API.appCode)&accid=\(Constants.API.accountId)&x={x}&y={y}&z={zoom}",
        subdomains: ["1", "2", "3"],
        minZoom: 1,
        maxZoom: 18,
        initialZoom: Constants.Map.defaultZoom,
        blueDotSize: Constants.Map.blueDotSize
    )
}
