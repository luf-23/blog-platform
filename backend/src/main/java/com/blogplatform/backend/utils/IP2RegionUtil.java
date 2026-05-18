package com.blogplatform.backend.utils;

import jakarta.annotation.PostConstruct;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class IP2RegionUtil {

    private Searcher searcher;

    // 特殊IP映射
    private static final Map<String, String> SPECIAL_IPS = new HashMap<>();
    static {
        SPECIAL_IPS.put("127.0.0.1", "本地回环地址");
        SPECIAL_IPS.put("0:0:0:0:0:0:0:1", "IPv6本地回环地址");
        SPECIAL_IPS.put("::1", "IPv6本地回环地址(简写)");
    }

    @PostConstruct
    public void init() throws IOException {
        // 从 classpath 加载 ip2region.xdb
        ClassPathResource resource = new ClassPathResource("IP2Region/ip2region.xdb");
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] dbBin = inputStream.readAllBytes();
            searcher = Searcher.newWithBuffer(dbBin);
        }
    }

    /**
     * 原始IP查询
     */
    public String search(String ip) {
        try {
            // 先检查特殊IP
            if (isSpecialIp(ip)) {
                return SPECIAL_IPS.getOrDefault(ip, "内网IP");
            }
            return searcher.search(ip);
        } catch (Exception e) {
            throw new RuntimeException("IP 查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化IP查询结果
     */
    public String getFormattedAddress(String ip) {
        // 检查特殊IP
        if (isSpecialIp(ip)) {
            return SPECIAL_IPS.getOrDefault(ip, "内网IP");
        }

        // 检查内网IP
        if (isPrivateIp(ip)) {
            return "内网IP";
        }

        // 正常IP查询
        String region;
        try {
            region = searcher.search(ip);
        } catch (Exception e) {
            return "IP定位失败";
        }

        // 解析原始数据
        String[] parts = region.split("\\|");
        if (parts.length < 5) {
            return "未知位置";
        }

        String country = parts[0];
        String regionName = parts[1];
        String province = parts[2];
        String city = parts[3];
        String isp = parts[4];

        // 构建详细地址
        StringBuilder address = new StringBuilder();

        // 国家/地区
        if (!"0".equals(country)) {
            address.append(country);
        } else {
            address.append("未知国家");
        }

        // 省份/直辖市
        if (!"0".equals(province) && !province.equals(city)) {
            address.append(" ").append(province);
        }

        // 城市
        if (!"0".equals(city)) {
            address.append(" ").append(city);
        }

        // 运营商
        if (!"0".equals(isp)) {
            address.append(" ").append(isp);
        }

        // 处理直辖市重复显示问题（如"北京 北京市"）
        String result = address.toString();
        if (province.equals(city) && !"0".equals(province)) {
            result = result.replace(province + " " + city, province);
        }

        return result.trim();
    }

    /**
     * 判断是否为特殊IP
     */
    private boolean isSpecialIp(String ip) {
        return SPECIAL_IPS.containsKey(ip);
    }

    /**
     * 判断是否为内网IP
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4内网地址判断
        if (ip.contains(".")) {
            if (ip.startsWith("10.") ||
                    ip.startsWith("192.168.") ||
                    ip.startsWith("172.") && isInPrivateRange(ip)) {
                return true;
            }
        }
        // IPv6内网地址判断
        else if (ip.startsWith("fc") || ip.startsWith("fd")) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为172.16.0.0 ~ 172.31.255.255范围内的IP
     */
    private boolean isInPrivateRange(String ip) {
        try {
            String[] segments = ip.split("\\.");
            if (segments.length >= 2) {
                int secondOctet = Integer.parseInt(segments[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    /**
     * 获取详细的地理位置信息（返回结构化数据）
     */
    public Map<String, String> getDetailedAddress(String ip) {
        Map<String, String> result = new HashMap<>();

        if (isSpecialIp(ip)) {
            result.put("ip", ip);
            result.put("type", "special");
            result.put("description", SPECIAL_IPS.get(ip));
            return result;
        }

        if (isPrivateIp(ip)) {
            result.put("ip", ip);
            result.put("type", "private");
            result.put("description", "内网IP");
            return result;
        }

        String region;
        try {
            region = searcher.search(ip);
        } catch (Exception e) {
            result.put("ip", ip);
            result.put("type", "error");
            result.put("description", "IP定位失败");
            return result;
        }

        String[] parts = region.split("\\|");
        result.put("ip", ip);
        result.put("type", "public");
        result.put("country", parts[0].equals("0") ? "" : parts[0]);
        result.put("region", parts[1].equals("0") ? "" : parts[1]);
        result.put("province", parts[2].equals("0") ? "" : parts[2]);
        result.put("city", parts[3].equals("0") ? "" : parts[3]);
        result.put("isp", parts[4].equals("0") ? "" : parts[4]);

        return result;
    }
}