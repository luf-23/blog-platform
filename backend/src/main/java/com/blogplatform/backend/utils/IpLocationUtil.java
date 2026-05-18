package com.blogplatform.backend.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class IpLocationUtil {

    //通过IP获取用户真实位置
    @Value("${gaode.ip.url}")
    private String GAODE_IP_URL;
    @Value("${gaode.ip.key}")
    private String GAODE_IP_KEY;
    @Value("${gaode.geocode.url}")
    private String GAODE_GEOCODE_URL;
    public List<Double> getLocationFromIp(String ip) {
        List<Double> location = new ArrayList<>();
        try {
            StringBuilder response = getStringBuilder(ip);
            JSONObject json = new JSONObject(response.toString());
            if ("1".equals(json.getString("status"))) {
                String locationStr = json.optString("rectangle", "");
                if (locationStr.isEmpty()||locationStr.equals("[]")) {
                    return null;
                }
                String[] coordinates = locationStr.split(";");
                Double longitude1 = Double.parseDouble(coordinates[0].split(",")[0]);
                Double longitude2 = Double.parseDouble(coordinates[1].split(",")[0]);
                Double latitude1 = Double.parseDouble(coordinates[0].split(",")[1]);
                Double latitude2 = Double.parseDouble(coordinates[1].split(",")[1]);
                Double longitude = (longitude1 + longitude2) / 2.0;
                Double latitude = (latitude1 + latitude2) / 2.0;
                location.add(longitude);
                location.add(latitude);
                return location;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAddressFromLocation(List< Double>  location){
        if (location == null || location.size() < 2) {
            return null;
        }

        double longitude = location.get(0);
        double latitude = location.get(1);
        String locationParam =  latitude + "," + longitude;

        try {
            JSONObject json = getJsonObject(locationParam);
            if ("1".equals(json.getString("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                return regeocode.getString("formatted_address"); // 返回完整地址
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "未知地址";
    }

    // 获取客户端真实IP的方法
    public String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理 IPv6 的本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private JSONObject getJsonObject(String locationParam) throws IOException, JSONException {
        URL url = new URL(GAODE_GEOCODE_URL + "?key=" + GAODE_IP_KEY + "&location=" + locationParam);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JSONObject json = new JSONObject(response.toString());
        return json;
    }


    private StringBuilder getStringBuilder(String ip) throws IOException {
        URL url = new URL(GAODE_IP_URL + "?key=" + GAODE_IP_KEY + "&ip=" + ip);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response;
    }
}
