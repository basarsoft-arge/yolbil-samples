import SwiftUI

/// Main container view for the map screen
struct MapContainerView: View {
    @StateObject private var viewModel = MapViewModel()
    @State private var showingAlert = false
    
    var body: some View {
        ZStack {
            // Map view
            YolbilMapView(viewModel: viewModel)
                .ignoresSafeArea()
            
            // Loading overlay
            if viewModel.isLoading {
                LoadingOverlayView()
            }
            
            // Zoom controls - right center
            HStack {
                Spacer()
                VStack {
                    Spacer()
                    ZoomControlsView(viewModel: viewModel)
                    Spacer()
                }
                .padding(.trailing, 20)
            }
            
            // Other control buttons - bottom right
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    MapControlsView(viewModel: viewModel)
                }
                .padding()
                .padding(.bottom, viewModel.searchResults.isEmpty ? 96 : 260)
            }

            // Navigation banner (top) - always visible while navigating
            if viewModel.isNavigating {
                VStack {
                    HStack(spacing: 12) {
                        Image(systemName: viewModel.navCommandIconSystemName ?? "arrow.up")
                            .foregroundColor(.white)
                        VStack(alignment: .leading, spacing: 4) {
                            if let dist = viewModel.navDistanceText, !dist.isEmpty {
                                Text(dist)
                                    .foregroundColor(.white)
                                    .font(.headline)
                            }
                            if let cmd = viewModel.navCommandText, !cmd.isEmpty {
                                Text(cmd)
                                    .foregroundColor(.white)
                                    .font(.subheadline)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)
                            }
                        }
                        Spacer()
                    }
                    .padding(12)
                    .background(Color.black.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 16)
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.easeInOut(duration: 0.25), value: viewModel.isNavigating)
            }

            // Coordinates pill temporarily disabled
            if false {
                VStack {
                    HStack {
                        if let loc = viewModel.currentLocation {
                            HStack(spacing: 6) {
                                Text(String(format: "%.5f, %.5f", loc.latitude, loc.longitude))
                                if let hdg = loc.headingDegrees { Text(String(format: "· %.0f°", hdg)) }
                                if let spd = loc.speedMps { Text(String(format: "· %.0f km/h", spd * 3.6)) }
                            }
                            .font(.footnote)
                            .foregroundColor(.white)
                            .padding(.vertical, 6)
                            .padding(.horizontal, 10)
                            .background(Color.black.opacity(0.6))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .padding(.leading, 16)
                            .padding(.top, 12)
                        }
                        Spacer()
                    }
                    Spacer()
                }
            }

            // Bottom overlay: Search or Navigation panel
            VStack(spacing: 8) {
                Spacer()
                if viewModel.isNavigating {
                    HStack { NavigationPanelView(viewModel: viewModel) }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                } else {
                    VStack(spacing: 8) {
                        HStack {
                            Image(systemName: "magnifyingglass")
                            TextField("Adres ara...", text: $viewModel.searchQuery, onCommit: {
                                viewModel.performGeocodingSearch()
                            })
                            .textFieldStyle(PlainTextFieldStyle())
                            .autocorrectionDisabled(true)
                            .textInputAutocapitalization(.never)
                            if viewModel.isSearching {
                                ProgressView().scaleEffect(0.8)
                            }
                            if !viewModel.searchQuery.isEmpty {
                                Button(action: { viewModel.searchQuery = ""; viewModel.searchResults = [] }) {
                                    Image(systemName: "xmark.circle.fill")
                                }
                            }
                        }
                        .padding(12)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)

                        if !viewModel.searchResults.isEmpty {
                            ScrollView {
                                VStack(alignment: .leading, spacing: 8) {
                                    ForEach(viewModel.searchResults) { item in
                                        Button(action: { viewModel.selectSearchResult(item) }) {
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(item.title).font(.subheadline).fontWeight(.semibold)
                                                if !item.subtitle.isEmpty {
                                                    Text(item.subtitle).font(.caption).foregroundColor(.secondary)
                                                }
                                            }
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.vertical, 8)
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                        Divider()
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                            .frame(maxHeight: 200)
                            .background(Color(UIColor.systemBackground))
                            .cornerRadius(12)
                            .shadow(radius: 4)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                    .background(
                        LinearGradient(gradient: Gradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.05)]), startPoint: .top, endPoint: .bottom)
                            .ignoresSafeArea(edges: .bottom)
                    )
                }
            }
        }
        .alert("Error", isPresented: $showingAlert) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.errorMessage ?? "An unknown error occurred")
        }
        .onChange(of: viewModel.errorMessage) { errorMessage in
            showingAlert = errorMessage != nil
        }
        .onAppear {
            viewModel.startLocationTracking()
        }
        .onDisappear {
            viewModel.stopLocationTracking()
        }
        // Custom bottom sheet overlay
        if viewModel.showRouteOptions, let selectedMarker = viewModel.selectedMarkerForRoute {
            VStack(spacing: 0) {
                // Reduced black overlay - only covers part of the screen
                Color.black.opacity(0.1)
                    .frame(maxHeight: 10)
                    .onTapGesture {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            viewModel.closeRouteOptions()
                        }
                    }
                    .transition(.opacity)
                
                // Bottom sheet content
                RouteOptionsBottomSheet(
                    viewModel: viewModel,
                    routingService: viewModel.routingService,
                    targetLocation: selectedMarker.coordinate,
                    isPresented: $viewModel.showRouteOptions
                )
                .transition(.move(edge: .bottom))
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
        }
    }
}
