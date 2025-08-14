package org.bobj.property.util;

public class GeoUtils {
    
    // 지구 반지름 (킬로미터)
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 위도와 경도의 차이를 라디안으로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        // 위도를 라디안으로 변환
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        // Haversine 공식 적용
        double a = Math.pow(Math.sin(dLat / 2), 2) + 
                   Math.pow(Math.sin(dLon / 2), 2) * 
                   Math.cos(lat1Rad) * Math.cos(lat2Rad);
        
        double c = 2 * Math.asin(Math.sqrt(a));
        
        return EARTH_RADIUS_KM * c;
    }

    public static boolean isWithinRadius(double centerLat, double centerLon, 
                                        double targetLat, double targetLon, 
                                        double radiusKm) {
        double distance = calculateDistance(centerLat, centerLon, targetLat, targetLon);
        return distance <= radiusKm;
    }
    
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }
}
