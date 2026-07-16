package com.example.jredirector.services;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import static com.example.jredirector.services.Utils.*;

@Service
public class ShortCodeService {

    private static final Logger logger = LoggerFactory.getLogger(ShortCodeService.class);

  public static final String RESOLVE_SHORT_CODE_SQL = """
    select original_url from urls where short_code = ?
  """;
  public static final String RECORD_VISIT_SQL = """
    insert into visits (short_code, ip_address, country, city, user_agent) values (?, ?, ?, ?, ?)
  """;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public Optional<String> get(String code) {
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    Object result = ops.get(key(code));
    if ((null == result) || result.toString().trim().isEmpty())
      return Optional.empty();

    return Optional.of(result.toString().trim());
  }

  public void set(String code, String originalUrl) {
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(key(code), originalUrl, Duration.ofDays(1));
  }

  public Optional<String> resolve(String code) {
    final var rs = jdbcTemplate.queryForRowSet(RESOLVE_SHORT_CODE_SQL, code);
    if (!rs.next())
      return Optional.empty();
    return Optional.of(rs.getString("original_url"));
  }

  public void recordVisit(String code, HttpServletRequest req) {
    String ip = getHeader("X-Forwarded-For", req.getRemoteAddr(), req);
    String userAgent = getHeader("User-Agent", "unknown", req);

    try {
      var geo = getLocate(ip);
      int count = jdbcTemplate.update(RECORD_VISIT_SQL, code, ip, geo.getString("country"), geo.getString("city"), userAgent);
      if (count > 0)
        logger.info("Updated visit {}/{}", geo.getString("country"), geo.getString("city"));
    } catch (Exception ex) {
      logger.error("Failed to record visit", ex);
    }
  }

  private final String key(String code) {
    return "urls:%s".formatted(code);
  }
}
