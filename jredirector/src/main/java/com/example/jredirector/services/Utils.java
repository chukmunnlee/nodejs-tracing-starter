package com.example.jredirector.services;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.http.HttpServletRequest;

public class Utils {

  private final static Logger logger = LoggerFactory.getLogger(Utils.class);

  public static final String getHeader(String headerName, String defaultValue, HttpServletRequest req) {
    String value = req.getHeader(headerName);
    if (Objects.isNull(value) || value.trim().isEmpty())
      return defaultValue;
    return value;
  }

  public static final JsonObject getLocate(String ip) {
    if (isPrivateIP(ip))
      return Json.createObjectBuilder()
          .add("country", "Unknown")
          .add("city", "Unknown")
          .build();

    try {
      var client = RestClient.create();
      var params = Map.of("fields", "status,country,city,message");
      var payload = client.get()
            .uri("http://ip-api.com/json/%s".formatted(ip), params)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);
      try (Reader r = new StringReader(payload)) {
        JsonReader reader = Json.createReader(r);
        JsonObject response = reader.readObject();
        if (response.containsKey("status") || (!response.getBoolean("status")))
          return Json.createObjectBuilder()
              .add("country", "Unknown")
              .add("city", "Unknown")
              .build();
        return response;
      }
    } catch (Exception ex) {
      logger.error("Exception when geolocatin IP: {}", ip, ex);
      return Json.createObjectBuilder()
          .add("country", "Unknown")
          .add("city", "Unknown")
          .build();
    }
  }

  public static final boolean isPrivateIP(String ip) {
    return ip.equals("127.0.0.1") ||
        ip.equals("::1") || ip.equals("::ffff:127.0.0.1") ||
        ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.168.");

  }
}
