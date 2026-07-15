package com.example.jredirector.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.json.Json;

@Controller
@RequestMapping
public class HealthController {

  @GetMapping(path="/health", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<String> health() {
    var ok = Json.createObjectBuilder()
        .add("status", "ok")
        .build().toString();
    return ResponseEntity.ok(ok);
  }
}
