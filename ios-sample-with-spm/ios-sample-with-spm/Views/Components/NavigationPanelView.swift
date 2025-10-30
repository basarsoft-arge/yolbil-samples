import SwiftUI

struct NavigationPanelView: View {
    @ObservedObject var viewModel: MapViewModel

    var body: some View {
        HStack(spacing: 12) {
            Button(action: {
                withAnimation(.easeInOut(duration: 0.25)) {
                    viewModel.stopNavigation()
                }
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "stop.fill")
                    Text("Bitir")
                }
                .font(.body)
                .padding(.vertical, 8)
                .padding(.horizontal, 14)
                .background(Color.red)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }

            Spacer()

            // Distance / ETA quick glance (do not show turn commands here)
            VStack(alignment: .trailing, spacing: 4) {
                HStack(spacing: 6) {
                    if let total = viewModel.navRemainingDistanceText, !total.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                            Text(total)
                        }
                        .font(.footnote)
                        .padding(.vertical, 6)
                        .padding(.horizontal, 10)
                        .background(Color.blue.opacity(0.12))
                        .foregroundColor(.blue)
                        .clipShape(Capsule())
                    } else if let dist = viewModel.navDistanceText, !dist.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "ruler")
                            Text(dist)
                        }
                        .font(.footnote)
                        .padding(.vertical, 6)
                        .padding(.horizontal, 10)
                        .background(Color.blue.opacity(0.12))
                        .foregroundColor(.blue)
                        .clipShape(Capsule())
                    }
                    if let eta = viewModel.navEtaText, !eta.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "clock")
                            Text(eta)
                        }
                        .font(.footnote)
                        .padding(.vertical, 6)
                        .padding(.horizontal, 10)
                        .background(Color.green.opacity(0.12))
                        .foregroundColor(.green)
                        .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(12)
        .background(Color(UIColor.systemBackground).opacity(0.95))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.08), radius: 6, x: 0, y: 2)
    }
}


