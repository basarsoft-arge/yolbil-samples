import SwiftUI

/// Modern bottom sheet for route options
struct RouteOptionsBottomSheet: View {
    @ObservedObject var viewModel: MapViewModel
    @ObservedObject var routingService: RoutingService
    let targetLocation: LocationData
    @Binding var isPresented: Bool
    
    @State private var selectedRouteType: RouteRequest.RouteType = .car
    @State private var isStartFromCurrentLocation: Bool = true
    @State private var sheetHeight: CGFloat = 0
    private let maxSheetHeightRatio: CGFloat = 0.34 // More compact: ~34% of the screen
    private let openAnimationDelay: Double = 0.18    // Bottom sheet open animation delay
    
    var body: some View {
        VStack(spacing: 0) {
            // Handle bar
            RoundedRectangle(cornerRadius: 3)
                .fill(Color.gray.opacity(0.4))
                .frame(width: 40, height: 6)
                .padding(.top, 8)
            
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 12) {
                    // Header
                    headerSection
                    
                    // From/To Section
                    fromToSection
                    
                    // Route Type Selection
                    routeTypeSection

                    // Route Action Button
                    actionButtonSection
                    
                    // Route Status & Info
                    routeStatusSection
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 12)
            }
        }
        .background(Color(UIColor.systemBackground))
        .cornerRadius(20, corners: [.topLeft, .topRight])
        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: -5)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear {
                        sheetHeight = min(proxy.size.height, UIScreen.main.bounds.height * maxSheetHeightRatio)
                    }
                    .onChange(of: proxy.size.height) { newHeight in
                        sheetHeight = min(newHeight, UIScreen.main.bounds.height * maxSheetHeightRatio)
                    }
            }
        )
        .onAppear {
            // Only disable follow mode (keep GPS running)
            viewModel.isFollowingUser = false
            // Defer map focusing until after open animation
            DispatchQueue.main.asyncAfter(deadline: .now() + openAnimationDelay) {
                let padding = sheetHeight > 0 ? sheetHeight : 340
                viewModel.fitToCurrentAnd(target: targetLocation, bottomPaddingPoints: padding, animated: true)
            }
        }
        .onChange(of: sheetHeight) { newHeight in
            guard newHeight > 0 else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                viewModel.fitToCurrentAnd(target: targetLocation, bottomPaddingPoints: newHeight, animated: true)
            }
        }
        .onChange(of: selectedRouteType) { _ in
            // When route type changes, clear current route so action becomes "Calculate Route" again
            routingService.clearRoute()
        }
    }
    
    // MARK: - Header Section
    private var headerSection: some View {
        HStack {
            Text("Rota Planla")
                .font(.headline)
                .fontWeight(.semibold)
            Spacer()
            Button(action: {
                withAnimation(.easeInOut(duration: 0.25)) {
                    viewModel.closeRouteOptions()
                }
            }) {
                Image(systemName: "xmark")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(6)
                    .background(Color.gray.opacity(0.12))
                    .clipShape(Circle())
            }
        }
        .padding(.top, 8)
    }
    
    // MARK: - From/To Section
    private var fromToSection: some View {
        VStack(spacing: 8) {
            // From Location (compact)
            HStack {
                Image(systemName: isStartFromCurrentLocation ? "location.fill" : "mappin.circle.fill")
                    .foregroundColor(isStartFromCurrentLocation ? .blue : .green)
                    .frame(width: 18)
                Text(isStartFromCurrentLocation ? "Başlangıç: Mevcut Konum" : "Başlangıç: İşaretli")
                    .font(.subheadline)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.gray.opacity(0.06))
            .cornerRadius(10)

            // Divider with arrow
            HStack {
                Rectangle()
                    .fill(Color.gray.opacity(0.25))
                    .frame(height: 1)
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isStartFromCurrentLocation.toggle()
                    }
                }) {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.blue)
                        .padding(6)
                        .background(Color.blue.opacity(0.12))
                        .clipShape(Circle())
                }
                Rectangle()
                    .fill(Color.gray.opacity(0.25))
                    .frame(height: 1)
            }

            // To Location (compact)
            HStack {
                Image(systemName: isStartFromCurrentLocation ? "mappin.circle.fill" : "location.fill")
                    .foregroundColor(isStartFromCurrentLocation ? .red : .blue)
                    .frame(width: 18)
                Text(isStartFromCurrentLocation ? String(format: "Hedef: %.4f, %.4f", targetLocation.latitude, targetLocation.longitude) : "Hedef: Mevcut Konum")
                    .font(.subheadline)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.gray.opacity(0.06))
            .cornerRadius(10)
        }
    }
    
    // MARK: - Route Type Section
    private var routeTypeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Ulaşım Türü")
                .font(.subheadline)
                .fontWeight(.semibold)
            Picker("Ulaşım Türü", selection: $selectedRouteType) {
                ForEach(RouteRequest.RouteType.allCases, id: \.self) { type in
                    Text(type.displayName).tag(type)
                }
            }
            .pickerStyle(SegmentedPickerStyle())
        }
    }
    
    // MARK: - Route Status Section
    private var routeStatusSection: some View {
        Group {
            switch routingService.routeState {
            case .idle:
                EmptyView()
                
            case .calculating:
                HStack(spacing: 8) {
                    ProgressView()
                        .scaleEffect(0.7)
                    Text("Rota hesaplanıyor...")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.blue.opacity(0.1))
                .cornerRadius(12)
                
            case .calculated(let route):
                RouteInfoCard(route: route)
                
            case .error(let message):
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.orange)
                    Text(message)
                        .font(.footnote)
                        .foregroundColor(.primary)
                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.orange.opacity(0.1))
                .cornerRadius(12)
            }
        }
    }
    
    // MARK: - Action Button Section
    private var actionButtonSection: some View {
        Group {
            if let _ = viewModel.currentLocation {
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        if case .calculated = routingService.routeState {
                            startNavigation()
                        } else {
                            calculateRoute()
                        }
                    }
                }) {
                    HStack(spacing: 12) {
                        if routingService.routeState == .calculating {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                            Text("Hesaplanıyor...")
                        } else if case .calculated = routingService.routeState {
                            Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                            Text("Navigasyonu Başlat")
                        } else {
                            Image(systemName: selectedRouteType.icon)
                            if case .calculated(let route) = routingService.routeState {
                                Text("Hesapla: \(route.distanceText) • \(route.durationText)")
                            } else {
                                Text("Rota Hesapla")
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(
                        Group {
                            if case .calculated = routingService.routeState {
                                Color.green
                            } else {
                                Color.blue
                            }
                        }
                    )
                    .foregroundColor(.white)
                    .cornerRadius(16)
                    .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
                }
                .disabled(routingService.routeState == .calculating)
            } else {
                Button(action: {
                    viewModel.startLocationTracking()
                }) {
                    HStack(spacing: 12) {
                        Image(systemName: "location.fill")
                        Text("Konum İzni Ver")
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.orange)
                    .foregroundColor(.white)
                    .cornerRadius(16)
                    .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
                }
            }
        }
    }
    
    // MARK: - Helper Methods
    private func calculateRoute() {
        guard let currentLocation = viewModel.currentLocation else { return }
        
        let startLocation = isStartFromCurrentLocation ? currentLocation : targetLocation
        let endLocation = isStartFromCurrentLocation ? targetLocation : currentLocation
        
        let routeRequest = RouteRequest(
            startLocation: startLocation,
            endLocation: endLocation,
            routeType: selectedRouteType
        )
        
        routingService.calculateRoute(request: routeRequest)
        // Fit both points with space for bottom sheet
        viewModel.fitToCurrentAnd(target: targetLocation, bottomPaddingPoints: 340, animated: true)
    }
    
    private func startNavigation() {
        guard let currentLocation = viewModel.currentLocation else { return }

        let startLocation = isStartFromCurrentLocation ? currentLocation : targetLocation
        let endLocation = isStartFromCurrentLocation ? targetLocation : currentLocation

        // Avoid degenerate routes
        if startLocation.latitude == endLocation.latitude && startLocation.longitude == endLocation.longitude {
            return
        }

        viewModel.startNavigation(from: startLocation, to: endLocation, routeType: selectedRouteType)
        
        withAnimation(.easeInOut(duration: 0.3)) {
            viewModel.closeRouteOptions()
        }
    }
}

// MARK: - Route Type Card
struct RouteTypeCard: View {
    let routeType: RouteRequest.RouteType
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                Image(systemName: routeType.icon)
                    .font(.title)
                    .foregroundColor(isSelected ? .white : .primary)
                    .opacity(isSelected ? 1.0 : 0.8)
                
                Text(routeType.displayName)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(isSelected ? .white : .primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                isSelected ? Color.blue : Color.gray.opacity(0.1)
            )
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Route Info Card
struct RouteInfoCard: View {
    let route: RouteInfo
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.title3)
                
                Text("Rota Hazır")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                Text(route.routeType.displayName)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue.opacity(0.1))
                    .foregroundColor(.blue)
                    .cornerRadius(8)
            }
            
            HStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Mesafe")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text(formatDistance(route.distance))
                        .font(.subheadline)
                        .fontWeight(.semibold)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Süre")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text(formatDuration(route.duration))
                        .font(.subheadline)
                        .fontWeight(.semibold)
                }
                
                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.green.opacity(0.1))
        .cornerRadius(12)
    }
    
    private func formatDistance(_ distance: Double) -> String {
        if distance < 1000 {
            return String(format: "%.0f m", distance)
        } else {
            return String(format: "%.1f km", distance / 1000)
        }
    }
    
    private func formatDuration(_ duration: Double) -> String {
        let minutes = Int(duration / 60)
        if minutes < 60 {
            return "\(minutes) dk"
        } else {
            let hours = minutes / 60
            let remainingMinutes = minutes % 60
            return "\(hours) sa \(remainingMinutes) dk"
        }
    }
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
