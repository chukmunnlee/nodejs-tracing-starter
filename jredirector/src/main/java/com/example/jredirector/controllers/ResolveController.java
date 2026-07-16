package com.example.jredirector.controllers;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.jredirector.services.ShortCodeService;

import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping
public class ResolveController {

  private final Logger logger = LoggerFactory.getLogger(ResolveController.class);

  @Autowired
  private ShortCodeService shortCodeSvc;

  @GetMapping(path="/resolve/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<String> resolve(@PathVariable String code, HttpServletRequest req) {

    logger.info("Resolve code: {}", code);

    try {
      Optional<String> opt = shortCodeSvc.get(code);
      if (opt.isPresent()) 
        return ResponseEntity.ok(
            Json.createObjectBuilder()
              .add("original_url", opt.get())
              .build().toString()
        );

      opt = shortCodeSvc.resolve(code);
      if (opt.isEmpty())
        return ResponseEntity.status(404)
            .body(
              Json.createObjectBuilder()
                .add("error", "Short URL not found")
                .build().toString()
        );

      shortCodeSvc.recordVisit(code, req);
      return ResponseEntity.ok(
          Json.createObjectBuilder()
            .add("original_url", opt.get())
            .build().toString()
      );
    } catch (Exception ex) {
      logger.error("Resolve failed", ex);
      return ResponseEntity.status(500)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Json.createObjectBuilder()
              .add("error", "Internal server error")
              .build().toString());
    }
  }
}
