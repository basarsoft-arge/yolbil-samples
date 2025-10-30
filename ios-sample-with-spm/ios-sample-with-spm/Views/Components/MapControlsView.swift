import SwiftUI

/// Zoom control buttons (separate component)
struct ZoomControlsView: View {
    @ObservedObject var viewModel: MapViewModel
    
    var body: some View {
        VStack(spacing: 4) {
            // Zoom in button
            Button(action: {
                viewModel.zoomIn()
            }) {
                Image(systemName: "plus")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Color.black.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .shadow(radius: 3)
            }
            
            // Separator line
            Rectangle()
                .fill(Color.white.opacity(0.3))
                .frame(width: 30, height: 1)
            
            // Zoom out button
            Button(action: {
                viewModel.zoomOut()
            }) {
                Image(systemName: "minus")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Color.black.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .shadow(radius: 3)
            }
        }
        .padding(8)
        .background(Color.black.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 4)
    }
}

/// Other control buttons for map interactions
struct MapControlsView: View {
    @ObservedObject var viewModel: MapViewModel
    
    var body: some View {
        // Location controls - simplified and modern
        VStack(spacing: 8) {
            // Center/Follow button (hidden when already following)
            if !viewModel.isFollowingUser || !viewModel.isLocationEnabled {
            Button(action: {
                if !viewModel.isLocationEnabled {
                    // Start GPS if not running
                    viewModel.startLocationTracking()
                } else {
                    // Ensure follow is enabled and center
                    viewModel.centerOnCurrentLocation()
                }
            }) {
                Image(systemName: viewModel.isFollowingUser ? "location.fill" : "location")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(viewModel.isFollowingUser ? Color.green : Color.gray.opacity(0.7))
                    .clipShape(Circle())
                    .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)
                    .overlay(
                        Circle()
                            .stroke(viewModel.isFollowingUser ? Color.green : Color.clear, lineWidth: 2)
                            .scaleEffect(viewModel.isFollowingUser ? 1.2 : 1.0)
                            .opacity(viewModel.isFollowingUser ? 0.6 : 0)
                            .animation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true), value: viewModel.isFollowingUser)
                    )
            }
            }
            
            // Developer Mode toggle
            Button(action: {
                viewModel.toggleDeveloperMode()
            }) {
                HStack(spacing: 6) {
                    Image(systemName: viewModel.developerModeEnabled ? "wrench.and.screwdriver.fill" : "wrench.adjustable")
                        .font(.system(size: 16, weight: .medium))
                    Text("Dev")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundColor(.white)
                .frame(width: 64, height: 36)
                .background(viewModel.developerModeEnabled ? Color.orange : Color.gray.opacity(0.7))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .shadow(color: .black.opacity(0.3), radius: 3, x: 0, y: 1)
            }

            // Removed clear markers button per requirement
        }
        }
    }


#Preview {
    ZoomControlsView(viewModel: MapViewModel())
}

#Preview {
    MapControlsView(viewModel: MapViewModel())
}
