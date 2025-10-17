import Foundation
import Combine
import YolbilMobileSDK

class RoutingService: ObservableObject {
    @Published var routeState: RouteState = .idle
    @Published var currentRoute: RouteInfo?
    
    private var cancellables = Set<AnyCancellable>()
    private var routingService: YBYolbilOnlineRoutingService?
    private var lastRequestId: UUID?
    
    init() {
        setupRoutingService()
    }
    
    private func setupRoutingService() {
        // YolbilOnlineRoutingService başlat
        // Constants'tan URL alınacak
        let baseUrl = "\(Constants.API.host)\(Constants.API.routingBasePath)?appcode=\(Constants.API.appCode)&accid=\(Constants.API.accountId)"
        
        routingService = YBYolbilOnlineRoutingService(baseUrl: baseUrl)
        print("RoutingService initialized successfully with URL: \(baseUrl)")
    }
    
    // MARK: - Route Calculation
    func calculateRoute(request: RouteRequest) {
        let requestId = UUID()
        lastRequestId = requestId
        guard let service = routingService else {
            routeState = .error("Routing service not available")
            return
        }
        
        routeState = .calculating
        currentRoute = nil
        
        print("Calculating route from \(request.startLocation.latitude),\(request.startLocation.longitude) to \(request.endLocation.latitude),\(request.endLocation.longitude)")
        
        // Use online-only request, do not draw on map
        requestRouteOnlineOnly(request: request) { [weak self] result in
            guard let self = self, self.lastRequestId == requestId else {
                print("Ignoring stale route result for requestId=\(requestId)")
                return
            }
            switch result {
            case .success(let routeInfo):
                self.routeState = .calculated(routeInfo)
                self.currentRoute = routeInfo
                print("Route calculated successfully: distance=\(routeInfo.distance) m, duration=\(routeInfo.duration) s")
            case .failure(let error):
                self.routeState = .error(error.localizedDescription)
                print("Route calculation failed: \(error.localizedDescription)")
            }
        }
    }
    
    private func performRouteCalculation(service: YBYolbilOnlineRoutingService, request: RouteRequest) {
        guard let projection = YBEPSG4326() else {
            DispatchQueue.main.async { [weak self] in
                self?.routeState = .error("Failed to create projection")
            }
            return
        }
        
        // Create routing request
        let routingPoints = YBMapPosVector()
        
        // Add start point
        if let startMapPos = request.startLocation.mapPosition(with: projection) {
            routingPoints?.add(startMapPos)
        }
        
        // Add end point
        if let endMapPos = request.endLocation.mapPosition(with: projection) {
            routingPoints?.add(endMapPos)
        }
        
        guard let points = routingPoints, points.size() >= 2 else {
            DispatchQueue.main.async { [weak self] in
                self?.routeState = .error("Invalid route points")
            }
            return
        }
        
        // FIXME: YBRoutingRequest constructor is not available in this SDK version
        // Using simulated route calculation for now
        print("Using simulated route calculation (YBRoutingRequest constructor not available)")
        simulateRouteCalculation(request: request)
    }

    // MARK: - Online route request (no drawing)
    /// Requests a route from Yolbil Online Routing Service without adding any layers/drawings.
    /// The result is returned via completion and internal route state is NOT modified.
    func requestRouteOnlineOnly(request: RouteRequest, completion: @escaping (Result<RouteInfo, Error>) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                // Prepare projection and points
                guard let projection = YBEPSG4326() else {
                    throw NSError(domain: "RoutingService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to create projection"])
                }
                guard let startPos = request.startLocation.mapPosition(with: projection),
                      let endPos = request.endLocation.mapPosition(with: projection) else {
                    throw NSError(domain: "RoutingService", code: -2, userInfo: [NSLocalizedDescriptionKey: "Invalid route points"])
                }

                // Log request details
                print("[RoutingRequest] baseUrl=\(Constants.API.host)\(Constants.API.routingBasePath) type=\(request.routeType.rawValue) from=\(request.startLocation.latitude),\(request.startLocation.longitude) to=\(request.endLocation.latitude),\(request.endLocation.longitude)")

                // Build a temporary navigation bundle to use its routing service (no layers will be added)
                let gpsSource = GPSLocationSource()
                let builder = YBYolbilNavigationBundleBuilder(
                    baseUrl: Constants.API.host,
                    accountId: Constants.API.accountId,
                    applicationCode: Constants.API.appCode,
                    locationSource: gpsSource
                )
                builder?.setRequestEndpoint(Constants.API.routingBasePath)

                // Configure route type flags
                switch request.routeType {
                case .car:
                    builder?.setIsCar(true)
                case .truck:
                    builder?.setIsTruck(true)
                case .pedestrian:
                    builder?.setIsPedestrian(true)
                }
                builder?.setAlternativeRoute(false)

                // Do NOT add layers to the map; just build and calculate
                guard let bundle = builder?.build() else {
                    throw NSError(domain: "RoutingService", code: -3, userInfo: [NSLocalizedDescriptionKey: "Failed to build navigation bundle"])
                }

                // Calculate route (this also prepares internal layers, but we don't add them to map)
                guard let results = bundle.startNavigation(startPos, to: endPos), results.size() > 0,
                      let navShared = results.get(0),
                      let navResult = YBNavigationResult.swigCreatePolymorphicInstance(navShared.getCptr(), swigOwnCObject: false) else {
                    throw NSError(domain: "RoutingService", code: -4, userInfo: [NSLocalizedDescriptionKey: "No route result"])
                }

                // Log response summary
                let respDistance = navResult.getTotalDistance()
                let respDuration = navResult.getTotalTime()
                let pointsCount = navResult.getPoints()?.size() ?? 0
                let instrCount = navResult.getInstructions()?.size() ?? 0
                print("[RoutingResponse] distance=\(respDistance) m, duration=\(respDuration) s, points=\(pointsCount), instructions=\(instrCount)")

                // Log detailed fields except line strings
                if let instructions = navResult.getInstructions() {
                    for i in 0..<(instructions.size()) {
                        if let ins = instructions.get(Int32(i)) {
                            let action = ins.getAction()
                            let street = ins.getStreetName() ?? ""
                            let text = ins.getInstruction() ?? ""
                            let turn = ins.getTurnAngle()
                            let azimuth = ins.getAzimuth()
                            let segDist = ins.getDistance()
                            let segTime = ins.getTime()
                            //print("[RoutingInstruction] #\(i) action=\(action) street=\(street) text=\(text) turn=\(turn) azimuth=\(azimuth) distance=\(segDist) time=\(segTime)")
                        }
                    }
                }

                // Convert to RouteInfo using existing converter
                let routeInfo = self.convertToRouteInfo(navResult, request: request)

                // Cleanup
                bundle.stopNavigation()

                DispatchQueue.main.async {
                    completion(.success(routeInfo))
                }
            } catch {
                print("[RoutingResponse] error=\(error.localizedDescription)")
                DispatchQueue.main.async {
                    completion(.failure(error))
                }
            }
        }
    }
    
    private func getRoutingProfile(for routeType: RouteRequest.RouteType) -> String {
        switch routeType {
        case .car:
            return "car" // or "driving-car"
        case .truck:
            return "truck" // truck routing profile
        case .pedestrian:
            return "foot" // or "foot-walking"
        }
    }
    
    private func handleRoutingResult(_ result: YBRoutingResult?, request: RouteRequest) {
        guard let result = result else {
            routeState = .error("Failed to calculate route")
            return
        }
        
        // Convert result to RouteInfo
        let routeInfo = convertToRouteInfo(result, request: request)
        
        routeState = .calculated(routeInfo)
        currentRoute = routeInfo
        print("Route calculated successfully: \(routeInfo.distanceText), \(routeInfo.durationText)")
    }
    
    private func convertToRouteInfo(_ result: YBRoutingResult, request: RouteRequest) -> RouteInfo {
        var routeGeometry: [LocationData] = []
        
        // Convert route points
        if let projection = result.getProjection(),
           let points = result.getPoints() {
            for i in 0..<points.size() {
                if let point = points.get(Int32(i)),
                   let wgs84Point = projection.toWgs84(point) {
                    let location = LocationData(
                        latitude: Double(wgs84Point.getY()),
                        longitude: Double(wgs84Point.getX())
                    )
                    routeGeometry.append(location)
                }
            }
        }
        
        // Convert instructions (simplified for now)
        var instructions: [RouteInstruction] = []
        if let routingInstructions = result.getInstructions() {
            for i in 0..<routingInstructions.size() {
                if let instruction = routingInstructions.get(Int32(i)) {
                    let routeInstruction = RouteInstruction(
                        text: instruction.getInstruction() ?? "Continue",
                        distance: Double(instruction.getDistance()),
                        duration: Double(instruction.getTime()),
                        location: routeGeometry.first ?? request.startLocation,
                        icon: "arrow.forward"
                    )
                    instructions.append(routeInstruction)
                }
            }
        }
        
        return RouteInfo(
            distance: result.getTotalDistance(),
            duration: result.getTotalTime(),
            routeType: request.routeType,
            routeGeometry: routeGeometry,
            instructions: instructions
        )
    }
    

    
    // MARK: - Route Management
    func clearRoute() {
        routeState = .idle
        currentRoute = nil
        lastRequestId = nil
        print("Route cleared")
    }
    
    func retryRouteCalculation() {
        guard case .error = routeState else { return }
        // Retry logic would go here
        print("Retrying route calculation...")
    }
    
    // MARK: - Route Display Helpers
    func createRouteLayer(route: RouteInfo) -> YBVectorLayer? {
        guard let projection = YBEPSG4326() else {
            print("Failed to create projection for route layer")
            return nil
        }
        
        guard let dataSource = YBLocalVectorDataSource(projection: projection) else {
            print("Failed to create data source for route layer")
            return nil
        }
        
        // Route line style
        let lineStyleBuilder = YBLineStyleBuilder()
        lineStyleBuilder?.setWidth(6.0)
        lineStyleBuilder?.setColor(YBColor(r: 0, g: 150, b: 255, a: 255)) // Blue route line
        
        guard let lineStyle = lineStyleBuilder?.buildStyle() else {
            print("Failed to create line style for route")
            return nil
        }
        
        // Create route geometry using YBMapPosVector
        let routePoints = YBMapPosVector()
        for location in route.routeGeometry {
            if let mapPos = location.mapPosition(with: projection) {
                routePoints?.add(mapPos)
            }
        }
        
        if let points = routePoints, points.size() >= 2 {
            let routeLine = YBLine(poses: points, style: lineStyle)
            dataSource.add(routeLine)
        }
        
        return YBVectorLayer(dataSource: dataSource)
    }
    
    // MARK: - Simulation (for testing)
    private func simulateRouteCalculation(request: RouteRequest) {
        // Simulate calculation delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            let mockDistance: Double
            let mockDuration: Double
            
            // Different values based on route type
            switch request.routeType {
            case .car:
                mockDistance = 15000.0 // 15 km
                mockDuration = 1200.0 // 20 minutes
            case .truck:
                mockDistance = 18000.0 // 18 km (longer route for trucks)
                mockDuration = 1800.0 // 30 minutes
            case .pedestrian:
                mockDistance = 12000.0 // 12 km (shorter walking route)
                mockDuration = 8400.0 // 2.3 hours
            }
            
            let mockRoute = RouteInfo(
                distance: mockDistance,
                duration: mockDuration,
                routeType: request.routeType,
                routeGeometry: [request.startLocation, request.endLocation],
                instructions: [
                    RouteInstruction(
                        text: "Başlangıç noktasından çıkış yapın",
                        distance: 0,
                        duration: 0,
                        location: request.startLocation,
                        icon: "location"
                    ),
                    RouteInstruction(
                        text: "Hedefe doğru \(request.routeType.displayName.lowercased()) ile ilerleyin",
                        distance: mockDistance,
                        duration: mockDuration,
                        location: request.endLocation,
                        icon: request.routeType.icon
                    )
                ]
            )
            
            self?.routeState = .calculated(mockRoute)
            self?.currentRoute = mockRoute
            print("Mock route calculated: \(mockDistance/1000) km, \(mockDuration/60) minutes")
        }
    }
}

// MARK: - Route Options
extension RoutingService {
    static func getAvailableRouteTypes() -> [RouteRequest.RouteType] {
        return RouteRequest.RouteType.allCases
    }
}
