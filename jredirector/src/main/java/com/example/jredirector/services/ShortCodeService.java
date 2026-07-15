package com.example.jredirector.services;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class ShortCodeService {

  @Autowired
  private RedisTemplate<String, Object> template;

  public Optional<String> get(String code) {
    ValueOperations<String, Object> ops = template.opsForValue();
    Object result = ops.get(key(code));
    if ((null == result) || result.toString().trim().isEmpty())
      return Optional.empty();

    return Optional.of(result.toString().trim());
  }

  public void set(String code, String originalUrl) {
    ValueOperations<String, Object> ops = template.opsForValue();
    ops.set(key(code), originalUrl, Duration.ofDays(1));
  }

  private final String key(String code) {
    return "urls:%s".formatted(code);
  }
}
