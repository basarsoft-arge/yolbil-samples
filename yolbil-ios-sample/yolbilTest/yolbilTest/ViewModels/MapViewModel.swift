import Foundation
import Combine
import SwiftUI
import UIKit
import YolbilMobileSDK
import INVSensorManager
import CoreLocation
import AVFoundation

/// View model for map-related functionality following MVVM pattern
@MainActor
class MapViewModel: ObservableObject {
    @Published var currentLocation: LocationData?
    @Published var isLocationEnabled: Bool = false
    @Published var errorMessage: String?
    @Published var isLoading: Bool = false
    @Published var showRouteOptions: Bool = false
    @Published var selectedMarkerForRoute: MapMarker?
    // Search UI state
    @Published var searchQuery: String = ""
    @Published var isSearching: Bool = false
    @Published var searchResults: [SearchResult] = []
    @Published var showSearchBar: Bool = true
    @Published var developerModeEnabled: Bool = false
    @Published var isFollowingUser: Bool = true
    @Published var isNavigating: Bool = false
    @Published var navBannerText: String?
    @Published var navDistanceText: String?
    @Published var navCommandText: String?
    @Published var navEtaText: String?
    @Published var navCommandIconSystemName: String?
    @Published var navRemainingDistanceText: String?
    
    private let mapService: MapService
    private let locationService: LocationService
    private let markerService: MarkerService
    let routingService: RoutingService // Public access for bottom sheet
    private var cancellables = Set<AnyCancellable>()
    
    private var mapView: YBMapView?
    private var blueDotLayer: YBVectorLayer?
    private var routeLayer: YBVectorLayer?
    private var navigationBundle: YBYolbilNavigationBundle?
    private var navigationLayer: YBLayer?
    private var narrator: AssetsVoiceNarrator?
    private var commandListener: YBCommandListener?
    private var skipNextLocationFocus: Bool = false
    private var navEtaTimer: Timer?
    private var navEtaTargetDate: Date?
    private var navEtaLastSeconds: Double?
    // Prevents triggering search when we programmatically update searchQuery (e.g., after selection)
    private var suppressSearchUpdates: Bool = false
    // Navigation instruction tracking for segment-based ETA
    private var navInstructionDurations: [Double] = []
    private var navInstructionTexts: [String] = []
    private var navInstructionIndex: Int = 0
    private var navTotalInstructionDuration: Double = 0
    
    init(
        mapService: MapService = MapService(),
        locationService: LocationService = LocationService(),
        markerService: MarkerService = MarkerService(),
        routingService: RoutingService = RoutingService()
    ) {
        self.mapService = mapService
        self.locationService = locationService
        self.markerService = markerService
        self.routingService = routingService
        
        setupBindings()
    }
    
    private func setupBindings() {
        // Bind location service to view model
        locationService.$currentLocation
            .receive(on: DispatchQueue.main)
            .assign(to: \.currentLocation, on: self)
            .store(in: &cancellables)
        
        locationService.$isLocationEnabled
            .receive(on: DispatchQueue.main)
            .assign(to: \.isLocationEnabled, on: self)
            .store(in: &cancellables)
        
        // Also listen to authorization status
        locationService.$authorizationStatus
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                switch status {
                case .denied, .restricted:
                    self?.errorMessage = "Location permission not granted. Please enable it in Settings."
                case .notDetermined:
                    print("Location permission not determined yet")
                default:
                    break
                }
            }
            .store(in: &cancellables)
        
        // Update map when location changes
        $currentLocation
            .compactMap { $0 }
            .sink { [weak self] location in
                self?.updateLocationOnMap(location)
            }
            .store(in: &cancellables)
        
        // Bind marker service changes for UI updates
        markerService.$markers
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                // Trigger objectWillChange for marker updates
                self?.objectWillChange.send()
            }
            .store(in: &cancellables)
        
        markerService.$singleMarkerMode
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                // Trigger objectWillChange for single marker mode changes
                self?.objectWillChange.send()
            }
            .store(in: &cancellables)

        // Listen to route state changes and apply to the map
        routingService.$routeState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                guard let self = self else { return }
                switch state {
                case .calculated(let route):
                    self.showRouteOnMap(route)
                case .idle, .error, .calculating:
                    self.removeRouteFromMap()
                }
            }
            .store(in: &cancellables)

        // Debounced search
        $searchQuery
            .removeDuplicates()
            .debounce(for: .milliseconds(350), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                guard let self = self else { return }
                if self.suppressSearchUpdates {
                    // Consume one programmatic change without searching
                    self.suppressSearchUpdates = false
                    return
                }
                self.performGeocodingSearch()
            }
            .store(in: &cancellables)
    }
    
    /// Creates and configures the map view
    func createMapView() -> YBMapView? {
        isLoading = true
        defer { isLoading = false }
        
        do {
            let mapView = try mapService.createMapView()
            self.mapView = mapView
            
            // Set initial location
            let initialLocation = currentLocation ?? LocationData.defaultLocation
            mapService.setMapFocus(mapView, to: initialLocation)
            
            // Add blue dot layer if location service is available
            if let locationSource = locationService.locationSource {
                blueDotLayer = mapService.createBlueDotLayer(
                    locationSource: locationSource,
                    initialLocation: initialLocation
                )
                
                if let blueDotLayer = blueDotLayer {
                    mapView.getLayers()?.add(blueDotLayer)
                }
            }
            
            // Add marker layer
            markerService.addMarkerLayerToMap(mapView)
            
            return mapView
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }
    
    /// Starts location tracking
    func startLocationTracking() {
        locationService.startLocationUpdates()
        
        // When starting tracking, focus closely on current location
        isFollowingUser = true
        if let location = currentLocation ?? LocationData.defaultLocation as LocationData?,
           let mapView = mapView {
            mapService.setMapFocus(mapView, to: location, zoom: Constants.Map.trackingZoom, animated: true)
        }
    }
    
    /// Stops location tracking
    func stopLocationTracking() {
        locationService.stopLocationUpdates()
        isFollowingUser = false
    }

    /// Toggle developer mode
    func toggleDeveloperMode() {
        developerModeEnabled.toggle()
        print("Developer mode is now: \(developerModeEnabled)")
        // In dev mode, disable CoreLocation updates so blue dot moves only by mock taps
        locationService.setCoreLocationActive(!developerModeEnabled)
        // Also disable SDK GPS hardware updates in dev mode; sendMockLocation keeps working
        locationService.setSDKGPSActive(!developerModeEnabled)
    }
    
    /// Updates the blue dot location on the map
    private func updateLocationOnMap(_ location: LocationData) {
        guard let mapView = mapView else { return }
        
        // After developer mock, skip focusing on this update
        if skipNextLocationFocus {
            skipNextLocationFocus = false
            return
        }

        // Only focus the camera on user's location in follow mode
        if isFollowingUser {
            mapService.setMapFocus(mapView, to: location, zoom: Constants.Map.trackingZoom, animated: true)
        }
    }
    
    /// Centers the map on current location with close zoom level
    func centerOnCurrentLocation() {
        guard let location = currentLocation ?? LocationData.defaultLocation as LocationData?,
              let mapView = mapView else { return }
        
        // Enable follow mode and use a closer zoom level (street level view)
        isFollowingUser = true
        mapService.setMapFocus(mapView, to: location, zoom: Constants.Map.closeZoom, animated: true)
    }
    
    /// Zooms in by one level
    func zoomIn() {
        guard let mapView = mapView else { return }
        mapService.zoomIn(mapView, animated: true)
    }
    
    /// Zooms out by one level
    func zoomOut() {
        guard let mapView = mapView else { return }
        mapService.zoomOut(mapView, animated: true)
    }
    
    /// Gets current zoom level
    func getCurrentZoom() -> Float? {
        guard let mapView = mapView else { return nil }
        return mapService.getCurrentZoom(mapView)
    }
    
    /// Sets a specific zoom level
    func setZoom(to level: Float) {
        guard let mapView = mapView else { return }
        mapService.setZoom(mapView, to: level, animated: true)
    }

    // MARK: - Route rendering
    private func showRouteOnMap(_ route: RouteInfo) {
        guard let mapView = mapView else { return }

        // Remove previous route layer if exists
        removeRouteFromMap()

        // Build route layer via routing service helper
        if let layer = routingService.createRouteLayer(route: route) {
            routeLayer = layer
            mapView.getLayers()?.add(layer)
            // Fit route into view
            fitRouteOnMap(route, bottomPaddingPoints: showRouteOptions ? 340 : 0)
        }
    }

    private func removeRouteFromMap() {
        guard let mapView = mapView, let layer = routeLayer else { return }
        _ = mapView.getLayers()?.remove(layer)
        routeLayer = nil
    }

    private func fitRouteOnMap(_ route: RouteInfo, bottomPaddingPoints: CGFloat = 0, animated: Bool = true) {
        guard let mapView = mapView,
              let options = mapView.getOptions(),
              let projection = options.getBaseProjection() else { return }

        var minX = Double.greatestFiniteMagnitude
        var minY = Double.greatestFiniteMagnitude
        var maxX = -Double.greatestFiniteMagnitude
        var maxY = -Double.greatestFiniteMagnitude

        for loc in route.routeGeometry {
            if let mapPos = loc.mapPosition(with: projection) {
                minX = min(minX, Double(mapPos.getX()))
                minY = min(minY, Double(mapPos.getY()))
                maxX = max(maxX, Double(mapPos.getX()))
                maxY = max(maxY, Double(mapPos.getY()))
            }
        }

        guard minX.isFinite, minY.isFinite, maxX.isFinite, maxY.isFinite else { return }

        let minPos = YBMapPos(x: minX, y: minY)
        let maxPos = YBMapPos(x: maxX, y: maxY)
        let bounds = YBMapBounds(min: minPos, max: maxPos)

        let scale = UIScreen.main.scale
        let screenSize = UIScreen.main.bounds.size
        let widthPx = Double(screenSize.width * scale)
        let heightPx = Double((screenSize.height * scale) - (bottomPaddingPoints * scale))
        // Add extra insets to see a bit wider and keep geometry inside safe frame
        let sidePaddingPx = Double(24.0 * scale)
        let topPaddingPx = Double(96.0 * scale)
        let minScreen = YBScreenPos(
            x: Float(max(0, sidePaddingPx)),
            y: Float(max(0, topPaddingPx))
        )
        let maxScreen = YBScreenPos(
            x: Float(max(0, widthPx - sidePaddingPx)),
            y: Float(max(0, heightPx - 32.0 * Double(scale)))
        )
        let screenBounds = YBScreenBounds(min: minScreen, max: maxScreen)

        let duration: Float = animated ? Constants.Map.animationDuration : 0
        mapView.move(toFit: bounds, screenBounds: screenBounds, integerZoom: true, durationSeconds: duration)
    }

    // MARK: - Navigation
    /// Starts Yolbil navigation by building a navigation bundle, adding its layers to the map and calculating the route
    func startNavigation(from start: LocationData, to end: LocationData, routeType: RouteRequest.RouteType) {
        guard let mapView = mapView,
              let options = mapView.getOptions(),
              let projection = options.getBaseProjection(),
              let locationSource = locationService.locationSource else {
            print("Navigation start failed: missing mapView/projection/locationSource")
            return
        }

        // Guard against identical or too-close points which cause <2 vertices errors
        if areLocationsTooClose(start, end) {
            print("Navigation start aborted: start and end are too close")
            return
        }

        // Stop any previous navigation and remove layers
        stopNavigation()

        // Prepare builder
        let builder = YBYolbilNavigationBundleBuilder(
            baseUrl: Constants.API.host,
            accountId: Constants.API.accountId,
            applicationCode: Constants.API.appCode,
            locationSource: locationSource
        )

        // Configure voice narrator if assets available
        configureVoiceNarratorIfAvailable(builder)

        // Attach UI command listener (to display commands on UI)
        let uiListener = UICommandListener()
        uiListener?.owner = self
        builder?.setCommandListener(uiListener)
        self.commandListener = uiListener

        // Configure routing
        builder?.setRequestEndpoint(Constants.API.routingBasePath)
        // Ask SDK to include manifest in responses if available (for richer metadata)
        builder?.setGetManifest(true)
        builder?.setAlternativeRoute(false)
        switch routeType {
        case .car:
            builder?.setIsCar(true)
        case .truck:
            builder?.setIsTruck(true)
        case .pedestrian:
            builder?.setIsPedestrian(true)
        }

        // Use our own blue dot layer to avoid duplicates (do not add bundle's blue dot layer)
        builder?.setBlueDotDataSourceEnabled(false)

        // Build bundle
        guard let bundle = builder?.build() else {
            print("Failed to build YolbilNavigationBundle")
            return
        }
        navigationBundle = bundle

        // Add only the navigation layer (skip null layers)
        if let layers = mapView.getLayers(), let navLayer = bundle.getNavigationLayer() {
            layers.add(navLayer)
            navigationLayer = navLayer
        }

        // Convert points to map projection
        guard let startPos = start.mapPosition(with: projection),
              let endPos = end.mapPosition(with: projection) else {
            print("Failed to convert start/end positions for navigation")
            return
        }

        // Start calculation and navigation on background
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let results = bundle.startNavigation(startPos, to: endPos), results.size() > 0,
                  let firstRaw = results.get(0),
                  let firstResult = YBNavigationResult.swigCreatePolymorphicInstance(firstRaw.getCptr(), swigOwnCObject: false) else {
                print("Navigation calculation returned no results")
                return
            }
            // Begin guidance so that manifest commands flow
            bundle.beginNavigation(firstResult)
            print("Navigation begun with first route result")
            // Clear any preview route layer on UI thread
            var totalSeconds = Double(firstResult.getTotalTime())
            // Prefer summing per-segment durations if available
            if let instructions = firstResult.getInstructions() {
                var sum: Double = 0
                var durations: [Double] = []
                var texts: [String] = []
                let count = Int(instructions.size())
                if count > 0 {
                    for i in 0..<count {
                        if let ins = instructions.get(Int32(i)) {
                            let t = Double(ins.getTime())
                            sum += t
                            durations.append(t)
                            texts.append(ins.getInstruction() ?? "")
                        }
                    }
                }
                if sum > 0 { totalSeconds = max(totalSeconds, sum) }
                // Store segment caches on main thread
                let totalDuration = durations.reduce(0, +)
                DispatchQueue.main.async { [weak self] in
                    self?.navInstructionDurations = durations
                    self?.navInstructionTexts = texts
                    self?.navInstructionIndex = 0
                    self?.navTotalInstructionDuration = totalDuration
                }
            }
            DispatchQueue.main.async {
                self?.isNavigating = true
                self?.routingService.clearRoute()
                self?.startEtaTimer(durationSeconds: totalSeconds)
                self?.navEtaLastSeconds = totalSeconds
                // If developer mode is on, ensure CoreLocation updates are disabled (mock-only)
                if self?.developerModeEnabled == true {
                    self?.locationService.setCoreLocationActive(false)
                    self?.locationService.setSDKGPSActive(false)
                }
            }
        }
    }

    /// Stops current navigation and removes navigation layers from the map
    func stopNavigation() {
        guard let mapView = mapView else { return }
        if let bundle = navigationBundle {
            bundle.stopNavigation()
        }
        if let layers = mapView.getLayers(), let navLayer = navigationLayer {
            _ = layers.remove(navLayer)
        }
        navigationLayer = nil
        navigationBundle = nil
        isNavigating = false
        stopEtaTimer()
        navBannerText = nil
        navDistanceText = nil
        navCommandText = nil
        navEtaText = nil
    }

    private func startEtaTimer(durationSeconds: Double) {
        guard durationSeconds.isFinite && durationSeconds > 0 else {
            navEtaText = nil
            return
        }
        navEtaTargetDate = Date().addingTimeInterval(durationSeconds)
        navEtaTimer?.invalidate()
        navEtaTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateEtaText()
        }
        RunLoop.main.add(navEtaTimer!, forMode: .common)
        updateEtaText()
        navEtaLastSeconds = durationSeconds
    }

    private func stopEtaTimer() {
        navEtaTimer?.invalidate()
        navEtaTimer = nil
        navEtaTargetDate = nil
    }

    private func updateEtaText() {
        guard let target = navEtaTargetDate else {
            navEtaText = nil
            return
        }
        let remaining = max(0, target.timeIntervalSinceNow)
        if remaining <= 1 {
            navEtaText = "Now"
            return
        }
        // Round up to minutes, never show seconds; minimum 1 minute when below 60s
        let minutesCeil = max(1, Int(ceil(remaining / 60)))
        if minutesCeil < 60 {
            navEtaText = "Kalan: \(minutesCeil) dk"
        } else {
            let hours = minutesCeil / 60
            let remMin = minutesCeil % 60
            navEtaText = "Kalan: \(hours) sa \(remMin) dk"
        }
    }

    /// Applies a mock location to SDK and updates ViewModel's currentLocation (Developer Mode)
    func applyDeveloperMockLocation(mapPos: YBMapPos) {
        guard let mapView = mapView, let projection = mapView.getOptions()?.getBaseProjection() else { return }
        // Send mock location to SDK GPS source
        locationService.locationSource?.sendMockLocation(mapPos)
        // Update view model current location for UI coherence (without refocus)
        if let wgs = projection.toWgs84(mapPos) {
            skipNextLocationFocus = true
            locationService.updateLocation(latitude: Double(wgs.getY()), longitude: Double(wgs.getX()))
        }
    }

    // MARK: - Navigation UI updates
    fileprivate func handleNavCommand(_ command: YBNavigationCommand) {
        let distanceMeters = max(0, Int(command.getDistanceToCommand()))
        navDistanceText = distanceMeters > 0 ? "\(distanceMeters) m" : "Now"
        var cmd = localizedDirection(from: command)
        // If SDK reports NO_COMMAND, prefer a friendly fallback instead of hiding
        if cmd.uppercased().contains("NO_COMMAND") || cmd.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            cmd = "Continue"
        }
        navCommandText = cmd
        navCommandIconSystemName = navigationIconName(from: command)
        // Update total remaining distance from navigation state
        updateRemainingDistanceFromState()
        // Update remaining time (ETA) from navigation state/command
        updateRemainingTimeFromState(command)
        // Keep banner text in sync for any consumers still using it
        navBannerText = [navDistanceText, navCommandText].compactMap { $0 }.joined(separator: ": ")
    }

    fileprivate func handleNavStarted() {
        navDistanceText = nil
        navCommandText = "Navigation started"
        navBannerText = navCommandText
    }

    fileprivate func handleNavWillRecalc() {
        navDistanceText = nil
        navCommandText = "Recalculating route..."
        navBannerText = navCommandText
    }

    fileprivate func handleNavRecalculated(_ result: YBNavigationResult?) {
        navDistanceText = nil
        navCommandText = "Route updated"
        navBannerText = navCommandText
        // If we received a new result, rebuild segment caches
        if let res = result, let instructions = res.getInstructions() {
            var durations: [Double] = []
            var texts: [String] = []
            let count = Int(instructions.size())
            if count > 0 {
                for i in 0..<count {
                    if let ins = instructions.get(Int32(i)) {
                        durations.append(Double(ins.getTime()))
                        texts.append(ins.getInstruction() ?? "")
                    }
                }
            }
            navInstructionDurations = durations
            navInstructionTexts = texts
            navInstructionIndex = 0
            navTotalInstructionDuration = durations.reduce(0, +)
        }
        navEtaLastSeconds = nil
        // Refresh remaining time after recalculation
        updateRemainingTimeFromState(nil)
    }

    fileprivate func handleNavStopped() {
        isNavigating = false
        stopEtaTimer()
        navDistanceText = nil
        navCommandText = nil
        navEtaText = nil
        navBannerText = nil
        navCommandIconSystemName = nil
        navRemainingDistanceText = nil
        // Clear navigation caches
        navInstructionDurations = []
        navInstructionTexts = []
        navInstructionIndex = 0
        navTotalInstructionDuration = 0
        navEtaLastSeconds = nil
    }

    /// Called when destination is reached; shows a short message, then stops navigation
    fileprivate func handleDestinationReached() {
        navDistanceText = nil
        navCommandText = "You have reached your destination"
        navBannerText = navCommandText
        navCommandIconSystemName = "checkmark"
        // Stop navigation shortly after informing the user
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.stopNavigation()
        }
    }

    private func navigationIconName(from command: YBNavigationCommand) -> String {
        let raw = (command.description() ?? "").uppercased()
        // U-turn
        if raw.contains("UTURN") { return "arrow.uturn.left" }
        // Roundabout
        if raw.contains("ROUNDABOUT") { return "arrow.triangle.2.circlepath" }
        // Right turns / continue right
        if raw.contains("TURN_RIGHT_SHARP") { return "arrowshape.turn.up.right" }
        if raw.contains("TURN_FAR_RIGHT") { return "arrowshape.turn.up.right" }
        if raw.contains("TURN_RIGHT_AT_THE_END_OF_ROAD") { return "arrowshape.turn.up.right" }
        if raw.contains("TURN_RIGHT_ONTO_ACCOMODATION") { return "arrowshape.turn.up.right" }
        if raw.contains("TURN_SECOND_RIGHT") || raw.contains("TURN_THIRD_RIGHT") { return "arrow.right" }
        if raw.contains("TURN_RIGHT") || raw.contains("CONTINUE_RIGHT") || raw.contains("STAY_RIGHT") { return "arrow.right" }
        // Left turns / continue left
        if raw.contains("TURN_LEFT_SHARP") { return "arrowshape.turn.up.left" }
        if raw.contains("TURN_FAR_LEFT") { return "arrowshape.turn.up.left" }
        if raw.contains("TURN_LEFT_AT_THE_END_OF_ROAD") { return "arrowshape.turn.up.left" }
        if raw.contains("TURN_LEFT_ONTO_ACCOMODATION") { return "arrowshape.turn.up.left" }
        if raw.contains("TURN_SECOND_LEFT") || raw.contains("TURN_THIRD_LEFT") { return "arrow.left" }
        if raw.contains("TURN_LEFT") || raw.contains("CONTINUE_LEFT") || raw.contains("STAY_LEFT") { return "arrow.left" }
        // Straight
        if raw.contains("GO_STRAIGHT") || raw.contains("CONTINUE_MIDDLE") { return "arrow.up" }
        // Default fallback
        return "arrow.up"
    }

    private func formatCommandText(_ text: String) -> String {
        let collapsed = text.components(separatedBy: .whitespacesAndNewlines).filter { !$0.isEmpty }.joined(separator: " ")
        if collapsed.count > 120 {
            let idx = collapsed.index(collapsed.startIndex, offsetBy: 120)
            return String(collapsed[..<idx]) + "…"
        }
        return collapsed
    }

    private func localizedDirection(from command: YBNavigationCommand) -> String {
        let raw = (command.description() ?? "").uppercased()
        // Roundabout exits
        if raw.contains("TAKE_FIRST_EXIT_ON_ROUNDABOUT") { return "Take the first exit at the roundabout" }
        if raw.contains("TAKE_SECOND_EXIT_ON_ROUNDABOUT") { return "Take the second exit at the roundabout" }
        if raw.contains("TAKE_THIRD_EXIT_ON_ROUNDABOUT") { return "Take the third exit at the roundabout" }
        if raw.contains("TAKE_FOURTH_EXIT_ON_ROUNDABOUT") { return "Take the fourth exit at the roundabout" }
        if raw.contains("TAKE_FIFTH_EXIT_ON_ROUNDABOUT") { return "Take the fifth exit at the roundabout" }
        if raw.contains("TAKE_SIXTH_EXIT_ON_ROUNDABOUT") { return "Take the sixth exit at the roundabout" }

        // Right turns
        if raw.contains("TURN_RIGHT_SHARP") { return "Turn sharply right" }
        if raw.contains("TURN_FAR_RIGHT") { return "Turn far right" }
        if raw.contains("TURN_SECOND_RIGHT") { return "Take the second right" }
        if raw.contains("TURN_THIRD_RIGHT") { return "Take the third right" }
        if raw.contains("TURN_RIGHT_AT_THE_END_OF_ROAD") { return "Turn right at the end of the road" }
        if raw.contains("TURN_RIGHT_ONTO_ACCOMODATION") { return "Turn right towards the accommodation" }
        if raw.contains("TURN_RIGHT") { return "Turn right" }

        // Left turns
        if raw.contains("TURN_LEFT_SHARP") { return "Turn sharply left" }
        if raw.contains("TURN_FAR_LEFT") { return "Turn far left" }
        if raw.contains("TURN_SECOND_LEFT") { return "Take the second left" }
        if raw.contains("TURN_THIRD_LEFT") { return "Take the third left" }
        if raw.contains("TURN_LEFT_AT_THE_END_OF_ROAD") { return "Turn left at the end of the road" }
        if raw.contains("TURN_LEFT_ONTO_ACCOMODATION") { return "Turn left towards the accommodation" }
        if raw.contains("TURN_LEFT") { return "Turn left" }

        // Stay/continue
        if raw.contains("STAY_RIGHT") { return "Keep right" }
        if raw.contains("CONTINUE_RIGHT") { return "Continue right" }
        if raw.contains("STAY_LEFT") { return "Keep left" }
        if raw.contains("CONTINUE_LEFT") { return "Continue left" }
        if raw.contains("CONTINUE_MIDDLE") { return "Continue straight (middle)" }

        // U-turn
        if raw.contains("UTURN") { return "Make a U-turn" }

        // Tunnels, over/under pass
        if raw.contains("ABOUT_THE_ENTER_TUNNEL") { return "You are about to enter a tunnel" }
        if raw.contains("IN_TUNNEL") { return "You are inside a tunnel" }
        if raw.contains("AFTER_TUNNEL") { return "Continue after the tunnel" }
        if raw.contains("UNDERPASS") { return "Go through the underpass" }
        if raw.contains("OVERPASS") { return "Go over the overpass" }

        // Other
        if raw.contains("PEDESTRIAN_ROAD") { return "Watch out for pedestrian road" }
        if raw.contains("SERVICE_ROAD") { return "Enter the service road" }
        if raw.contains("EXCEEDED_THE_SPEED_LIMIT") { return "You exceeded the speed limit" }
        if raw.contains("WILL_REACH_YOUR_DESTINATION") { return "You are about to reach your destination" }
        if raw.contains("REACHED_YOUR_DESTINATION") { return "You have reached your destination" }
        if raw.contains("GO_STRAIGHT") { return "Go straight" }

        // Fallback to cleaned description
        return formatCommandText(command.description() ?? "")
    }

    // MARK: - UICommandListener
    private class UICommandListener: YBCommandListener {
        weak var owner: MapViewModel?

        override func onCommandReady(_ command: YBNavigationCommand!) -> Bool {
            guard let command = command else { return false }
            DispatchQueue.main.async { [weak self] in
                // Auto-stop when destination reached
                let raw = (command.description() ?? "").uppercased()
                if raw.contains("REACHED_YOUR_DESTINATION") {
                    self?.owner?.handleDestinationReached()
                    return
                }
                self?.owner?.handleNavCommand(command)
            }
            return false // do not consume, allow narrator to handle as well
        }

        override func onNavigationStarted() -> Bool {
            DispatchQueue.main.async { [weak self] in
                self?.owner?.handleNavStarted()
            }
            return false
        }

        override func onNavigationWillRecalculate() -> Bool {
            DispatchQueue.main.async { [weak self] in
                self?.owner?.handleNavWillRecalc()
            }
            return false
        }

        override func onNavigationRecalculated(_ navigationResult: YBNavigationResult!) -> Bool {
            DispatchQueue.main.async { [weak self] in
                self?.owner?.handleNavRecalculated(navigationResult)
            }
            return false
        }

        override func onNavigationStopped() -> Bool {
            DispatchQueue.main.async { [weak self] in
                self?.owner?.handleNavStopped()
            }
            return false
        }

        override func onLocationChanged(_ command: YBNavigationCommand!) -> Bool {
            // We can also update remaining distance as the user moves
            guard let command = command else { return false }
            DispatchQueue.main.async { [weak self] in
                // Auto-stop when destination reached
                let raw = (command.description() ?? "").uppercased()
                if raw.contains("REACHED_YOUR_DESTINATION") {
                    self?.owner?.handleDestinationReached()
                    return
                }
                self?.owner?.handleNavCommand(command)
                self?.owner?.updateRemainingDistanceFromState()
                self?.owner?.updateRemainingTimeFromState(command)
            }
            return false
        }
    }

    // MARK: - Remaining distance (total)
    private func updateRemainingDistanceFromState() {
        guard let bundle = navigationBundle,
              let nav = bundle.getNavigation(),
              let state = nav.getState() else { return }
        let remainingMeters = Double(state.getDistanceToTarget())
        navRemainingDistanceText = formatDistance(remainingMeters)
    }

    private func formatDistance(_ meters: Double) -> String {
        if meters < 1000 {
            return String(format: "%.0f m", meters)
        } else {
            return String(format: "%.1f km", meters / 1000)
        }
    }

    // MARK: - Remaining time (ETA)
    private func updateRemainingTimeFromState(_ command: YBNavigationCommand? = nil) {
        var remainingSeconds: Double?
        var currentIdx = navInstructionIndex

        if !navInstructionDurations.isEmpty {
            let count = navInstructionDurations.count
            currentIdx = min(max(currentIdx, 0), max(count - 1, 0))

            if let cmd = command {
                let key = normalizedCommandKey(cmd.description() ?? "")
                if navInstructionIndex < navInstructionTexts.count {
                    var matchedIndex: Int?
                    for offset in 0..<min(3, navInstructionTexts.count - navInstructionIndex) {
                        let idx = navInstructionIndex + offset
                        if normalizedCommandKey(navInstructionTexts[idx]) == key {
                            matchedIndex = idx
                            break
                        }
                    }
                    if matchedIndex == nil {
                        let limit = min(navInstructionTexts.count, navInstructionIndex + 9)
                        for idx in (navInstructionIndex + 3)..<limit {
                            if normalizedCommandKey(navInstructionTexts[idx]) == key {
                                matchedIndex = idx
                                break
                            }
                        }
                    }
                    if matchedIndex == nil, currentIdx > 0 {
                        let backLimit = max(0, currentIdx - 2)
                        for idx in (backLimit..<currentIdx).reversed() {
                            if normalizedCommandKey(navInstructionTexts[idx]) == key {
                                matchedIndex = idx
                                break
                            }
                        }
                    }
                    if let match = matchedIndex {
                        navInstructionIndex = match
                        currentIdx = match
                    }
                }

                let toNext = max(0.0, cmd.getRemainingTimeInSec())
                let restStart = min(currentIdx + 1, count)
                let rest = restStart < count ? navInstructionDurations[restStart...].reduce(0, +) : 0
                let total = toNext + rest
                if total > 0 { remainingSeconds = total }
            } else {
                let rest = currentIdx < count ? navInstructionDurations[currentIdx...].reduce(0, +) : 0
                if rest > 0 { remainingSeconds = rest }
            }
        }

        var stateEstimate: Double?
        if let bundle = navigationBundle, let nav = bundle.getNavigation(), let state = nav.getState() {
            let stateRemaining = Double(state.getRemainingTimeInSeconds())
            if stateRemaining.isFinite && stateRemaining > 0 {
                stateEstimate = stateRemaining
            }
        }

        if let cmd = command {
            let cmdRemaining = cmd.getRemainingTimeInSec()
            if cmdRemaining.isFinite && cmdRemaining > 0 {
                if let existing = remainingSeconds {
                    remainingSeconds = min(existing, cmdRemaining)
                } else {
                    remainingSeconds = cmdRemaining
                }
            }
        }

        if let state = stateEstimate {
            if let existing = remainingSeconds {
                remainingSeconds = min(existing, state)
            } else {
                remainingSeconds = state
            }
        }

        guard var finalSeconds = remainingSeconds, finalSeconds > 0 else { return }

        if navTotalInstructionDuration > 0 {
            finalSeconds = min(finalSeconds, navTotalInstructionDuration)
        }

        if let last = navEtaLastSeconds, last > 0 {
            finalSeconds = min(finalSeconds, last)
        }

        // Ensure timer exists; if already running, just update target
        if navEtaTimer == nil {
            startEtaTimer(durationSeconds: finalSeconds)
            navEtaLastSeconds = finalSeconds
        } else {
            navEtaTargetDate = Date().addingTimeInterval(finalSeconds)
            updateEtaText()
            navEtaLastSeconds = finalSeconds
        }
    }

    private func normalizedCommandKey(_ text: String) -> String {
        let u = text.uppercased()
        let allowed = u.filter { $0.isLetter || $0.isNumber || $0 == " " }
        return allowed.replacingOccurrences(of: " ", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - Voice guidance
    private func configureVoiceNarratorIfAvailable(_ builder: YBYolbilNavigationBundleBuilder?) {
        // Setup audio session for playback (non-blocking)
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Audio session setup failed: \(error.localizedDescription)")
        }

        // Load voice asset package from app bundle
        if let zipData = YBAssetUtils.loadAsset("CommandVoices.zip"),
           let assetPackage = YBZippedAssetPackage(zip: zipData) {
            let voiceNarrator = AssetsVoiceNarrator(voiceAssets: assetPackage)
            builder?.setNarrator(voiceNarrator)
            // Keep strong reference to avoid ARC releasing it
            self.narrator = voiceNarrator
            print("Voice narrator configured with CommandVoices.zip")
        } else {
            print("Voice assets not found. Add CommandVoices.zip to the app bundle to enable guidance.")
        }
    }

    /// Rough haversine check to avoid degenerate routes
    private func areLocationsTooClose(_ a: LocationData, _ b: LocationData, thresholdMeters: Double = 5.0) -> Bool {
        let earthRadius = 6371000.0
        let dLat = (b.latitude - a.latitude) * .pi / 180
        let dLon = (b.longitude - a.longitude) * .pi / 180
        let lat1 = a.latitude * .pi / 180
        let lat2 = b.latitude * .pi / 180
        let sinDlat = sin(dLat / 2)
        let sinDlon = sin(dLon / 2)
        let h = sinDlat * sinDlat + cos(lat1) * cos(lat2) * sinDlon * sinDlon
        let c = 2 * atan2(sqrt(h), sqrt(1 - h))
        let distance = earthRadius * c
        return distance < thresholdMeters
    }

    // MARK: - Geocoding (Address Search)
    struct SearchResult: Identifiable, Equatable {
        let id = UUID()
        let title: String
        let subtitle: String
        let location: LocationData
    }

    func performGeocodingSearch() {
        guard !searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            searchResults = []
            return
        }
        guard searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).count >= 3 else {
            // Do not search for very short queries
            return
        }
        guard let mapView = mapView, let projection = mapView.getOptions()?.getBaseProjection() else { return }

        isSearching = true
        let query = searchQuery
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            
            let service = YBYolbilOnlineGeocodingService(accId: Constants.API.accountId, appCode: Constants.API.appCode)
            let request = YBGeocodingRequest(projection: projection, query: query)
            if let results = service?.calculateAddresses(request) {
                var items: [SearchResult] = []
                let count = Int(results.size())
                for i in 0..<count {
                    guard let res = results.get(Int32(i)) else { continue }
                    let address = res.getAddress()
                    let name = address?.getName() ?? address?.getStreet() ?? ""
                    let detailParts: [String] = [
                        address?.getStreet() ?? "",
                        address?.getHouseNumber() ?? "",
                        address?.getLocality() ?? "",
                        address?.getCountry() ?? ""
                    ].filter { !$0.isEmpty }
                    let subtitle = detailParts.joined(separator: ", ")

                    var loc = LocationData(latitude: 0, longitude: 0)
                    if let fc = res.getFeatureCollection(), fc.getFeatureCount() > 0,
                       let geom = fc.getFeature(0)?.getGeometry(),
                       let pt = YBPointGeometry.swigCreatePolymorphicInstance(geom.getCptr(), swigOwnCObject: false),
                       let pos = pt.getPos() {
                        loc = LocationData(latitude: Double(pos.getY()), longitude: Double(pos.getX()))
                    }
                    items.append(SearchResult(title: name.isEmpty ? subtitle : name, subtitle: subtitle, location: loc))
                }
                DispatchQueue.main.async {
                    self.isSearching = false
                    self.searchResults = items
                }
            } else {
                DispatchQueue.main.async {
                    self.isSearching = false
                    self.searchResults = []
                }
            }
            
        }
    }

    /// Dismisses the keyboard and hides the search results list
    func dismissSearchUIAndKeyboard() {
        // Hide keyboard
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        // Hide search list
        isSearching = false
        searchResults = []
    }

    func selectSearchResult(_ result: SearchResult) {
        // Add marker and focus map
        let marker = MapMarker(coordinate: result.location, title: result.title, subtitle: result.subtitle)
        markerService.addMarker(marker)
        if let mapView = mapView {
            mapService.setMapFocus(mapView, to: result.location, zoom: Constants.Map.closeZoom, animated: true)
        }
        // Optionally close list
        searchResults = []
        isSearching = false
        suppressSearchUpdates = true
        searchQuery = result.title
        
        // Dismiss keyboard if open
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)

        // Open route planning bottom sheet for the selected marker
        showRouteOptionsForMarker(marker)
    }

    /// Fits the map to show both current location and a target location, respecting a bottom sheet padding
    func fitToCurrentAnd(target: LocationData, bottomPaddingPoints: CGFloat = 320, animated: Bool = true) {
        guard let mapView = mapView,
              let options = mapView.getOptions(),
              let projection = options.getBaseProjection() else { return }

        let start = currentLocation ?? LocationData.defaultLocation
        guard let pos1 = start.mapPosition(with: projection),
              let pos2 = target.mapPosition(with: projection) else { return }

        let minX = min(pos1.getX(), pos2.getX())
        let minY = min(pos1.getY(), pos2.getY())
        let maxX = max(pos1.getX(), pos2.getX())
        let maxY = max(pos1.getY(), pos2.getY())

        let minPos = YBMapPos(x: minX, y: minY)
        let maxPos = YBMapPos(x: maxX, y: maxY)
        let bounds = YBMapBounds(min: minPos, max: maxPos)

        let scale = UIScreen.main.scale
        let screenSize = UIScreen.main.bounds.size
        let widthPx = Float(screenSize.width * scale)
        let heightPx = Float((screenSize.height * scale) - (bottomPaddingPoints * scale))
        // Add extra insets for a slightly wider view and to avoid clipping near edges
        let sidePaddingPx = Float(24.0 * scale)
        let topPaddingPx = Float(96.0 * scale)
        let bottomExtraPx = Float(32.0 * scale)
        let minScreen = YBScreenPos(x: sidePaddingPx, y: topPaddingPx)
        let maxScreen = YBScreenPos(x: max(0, widthPx - sidePaddingPx), y: max(0, heightPx - bottomExtraPx))
        let screenBounds = YBScreenBounds(min: minScreen, max: maxScreen)

        let duration: Float = animated ? Constants.Map.animationDuration : 0
        mapView.move(toFit: bounds, screenBounds: screenBounds, integerZoom: true, durationSeconds: duration)
    }
    
    /// Adds a marker at the specified location (called by long press)
    func addMarkerAtLocation(_ location: LocationData) {
        // Clear previous routing processes if in single marker mode
        if markerService.singleMarkerMode && !markerService.markers.isEmpty {
            closeRouteOptions()
            routingService.clearRoute()
            print("Previous marker replaced - routing processes cleared")
        }
        
        let marker = MapMarker.longPressMarker(at: location)
        markerService.addMarker(marker)
        
        // Set marker for route options and show bottom sheet with animation
        selectedMarkerForRoute = marker
        withAnimation(.easeInOut(duration: 0.3)) {
            showRouteOptions = true
        }
        
        print("Marker added at: \(location.latitude), \(location.longitude)")
    }
    
    /// Removes all markers from the map
    func removeAllMarkers() {
        markerService.removeAllMarkers()
        
        // Clear routing processes when markers are removed
        closeRouteOptions()
        routingService.clearRoute()
        
        print("All markers removed - routing processes cleared")
    }
    
    /// Gets all current markers
    var currentMarkers: [MapMarker] {
        return markerService.markers
    }
    
    /// Removes a specific marker
    func removeMarker(_ marker: MapMarker) {
        markerService.removeMarker(marker)
        
        // Clear routing processes if the removed marker was selected for routing
        if let selectedMarker = selectedMarkerForRoute, selectedMarker.id == marker.id {
            closeRouteOptions()
            routingService.clearRoute()
            print("Selected marker removed - routing processes cleared")
        }
    }
    
    /// Gets single marker mode status
    var isSingleMarkerMode: Bool {
        return markerService.singleMarkerMode
    }
    
    /// Toggles single marker mode
    func toggleSingleMarkerMode() {
        // Clear routing processes when changing marker mode
        closeRouteOptions()
        routingService.clearRoute()
        
        markerService.toggleSingleMarkerMode()
        
        print("Marker mode toggled - routing processes cleared")
    }
    
    /// Clears any error messages
    func clearError() {
        errorMessage = nil
    }
    
    // MARK: - Route Options Methods
    
    /// Closes route options bottom sheet
    func closeRouteOptions() {
        withAnimation(.easeInOut(duration: 0.3)) {
            showRouteOptions = false
        }
        selectedMarkerForRoute = nil
        routingService.clearRoute()
    }
    
    /// Shows route options for a specific marker
    func showRouteOptionsForMarker(_ marker: MapMarker) {
        selectedMarkerForRoute = marker
        withAnimation(.easeInOut(duration: 0.3)) {
            showRouteOptions = true
        }
    }
}

