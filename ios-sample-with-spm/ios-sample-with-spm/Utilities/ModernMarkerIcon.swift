import UIKit
import YolbilMobileSDK

/// Modern marker icon generator
class ModernMarkerIcon {
    
    /// Creates a modern pin-style marker icon
    static func createPinIcon(color: UIColor, size: CGFloat = 40.0) -> UIImage? {
        let iconSize = CGSize(width: size, height: size * 1.3) // Pin shape is taller
        let pinSize = CGSize(width: size * 0.8, height: size)
        
        UIGraphicsBeginImageContextWithOptions(iconSize, false, 0)
        defer { UIGraphicsEndImageContext() }
        
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        
        // Pin body (circle at top)
        let circleRadius = pinSize.width * 0.35
        let circleCenter = CGPoint(x: iconSize.width / 2, y: circleRadius + 6)
        
        // Shadow
        context.saveGState()
        context.setShadow(offset: CGSize(width: 0, height: 2), blur: 4, color: UIColor.black.withAlphaComponent(0.3).cgColor)
        
        // Pin shape path
        let pinPath = UIBezierPath()
        
        // Circle part
        pinPath.addArc(withCenter: circleCenter, radius: circleRadius, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        
        // Bottom point - exact tip of the pin (critical for anchor point)
        let bottomPoint = CGPoint(x: iconSize.width / 2, y: iconSize.height - 2) // 2px margin from bottom edge
        pinPath.move(to: CGPoint(x: circleCenter.x - circleRadius * 0.4, y: circleCenter.y + circleRadius * 0.8))
        pinPath.addLine(to: bottomPoint)
        pinPath.addLine(to: CGPoint(x: circleCenter.x + circleRadius * 0.4, y: circleCenter.y + circleRadius * 0.8))
        pinPath.close()
        
        // Fill with gradient
        color.setFill()
        pinPath.fill()
        
        context.restoreGState()
        
        // Inner circle (white)
        let innerCircleRadius = circleRadius * 0.4
        let innerCirclePath = UIBezierPath(arcCenter: circleCenter, radius: innerCircleRadius, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        UIColor.white.setFill()
        innerCirclePath.fill()
        
        // Highlight effect
        let highlightPath = UIBezierPath(arcCenter: CGPoint(x: circleCenter.x - circleRadius * 0.3, y: circleCenter.y - circleRadius * 0.3), radius: circleRadius * 0.2, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        UIColor.white.withAlphaComponent(0.6).setFill()
        highlightPath.fill()
        
        return UIGraphicsGetImageFromCurrentImageContext()
    }
    
    /// Creates a modern circle marker icon
    static func createCircleIcon(color: UIColor, size: CGFloat = 32.0) -> UIImage? {
        let iconSize = CGSize(width: size, height: size)
        
        UIGraphicsBeginImageContextWithOptions(iconSize, false, 0)
        defer { UIGraphicsEndImageContext() }
        
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        
        let center = CGPoint(x: iconSize.width / 2, y: iconSize.height / 2)
        let radius = size * 0.4
        
        // Shadow
        context.saveGState()
        context.setShadow(offset: CGSize(width: 0, height: 1), blur: 3, color: UIColor.black.withAlphaComponent(0.25).cgColor)
        
        // Outer circle
        let outerCircle = UIBezierPath(arcCenter: center, radius: radius, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        color.setFill()
        outerCircle.fill()
        
        context.restoreGState()
        
        // Inner circle (white)
        let innerCircle = UIBezierPath(arcCenter: center, radius: radius * 0.5, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        UIColor.white.setFill()
        innerCircle.fill()
        
        // Border
        let borderCircle = UIBezierPath(arcCenter: center, radius: radius, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        borderCircle.lineWidth = 2
        UIColor.white.setStroke()
        borderCircle.stroke()
        
        return UIGraphicsGetImageFromCurrentImageContext()
    }
    
    /// Creates a modern location dot icon (GPS style)
    static func createLocationDotIcon(color: UIColor, size: CGFloat = 24.0) -> UIImage? {
        let iconSize = CGSize(width: size, height: size)
        
        UIGraphicsBeginImageContextWithOptions(iconSize, false, 0)
        defer { UIGraphicsEndImageContext() }
        
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        
        let center = CGPoint(x: iconSize.width / 2, y: iconSize.height / 2)
        let radius = size * 0.45
        
        // Outer ring (pulse effect)
        let outerRing = UIBezierPath(arcCenter: center, radius: radius, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        color.withAlphaComponent(0.3).setFill()
        outerRing.fill()
        
        // Main dot
        let mainDot = UIBezierPath(arcCenter: center, radius: radius * 0.6, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        color.setFill()
        mainDot.fill()
        
        // Inner dot (bright)
        let innerDot = UIBezierPath(arcCenter: center, radius: radius * 0.3, startAngle: 0, endAngle: 2 * .pi, clockwise: true)
        UIColor.white.setFill()
        innerDot.fill()
        
        return UIGraphicsGetImageFromCurrentImageContext()
    }
    
    /// Converts UIImage to YBBitmap for use with YolbilMobileSDK
    static func createYBBitmap(from image: UIImage) -> YBBitmap? {
        return YBBitmapUtils.createBitmap(from: image)
    }
}

/// Extensions for converting MarkerColor to UIColor
extension MarkerColor {
    var uiColor: UIColor {
        let rgb = self.rgbValue
        return UIColor(red: CGFloat(rgb.r), green: CGFloat(rgb.g), blue: CGFloat(rgb.b), alpha: CGFloat(rgb.a))
    }
}
