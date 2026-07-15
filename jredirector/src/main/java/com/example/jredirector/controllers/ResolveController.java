package com.example.jredirector.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.jredirector.services.ShortCodeService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping
public class ResolveController {

  private final Logger logger = LoggerFactory.getLogger(ResolveController.class);

  @Autowired
  private ShortCodeService shortCodeSvc;

  @GetMapping(path="/resolve/{code}")
  @ResponseBody
  public ResponseEntity<String> resolve(@PathVariable String code, HttpServletRequest req) {

    logger.info("Resolve code: {}", code);

    return ResponseEntity.ok("");
  }
}
