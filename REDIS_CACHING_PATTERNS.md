# Redis Caching Patterns - Complete Guide

## 📚 Table of Contents
- [Redis Basics](#redis-basics)
- [Caching Patterns](#caching-patterns)
- [Implementation with Spring](#implementation-with-spring)
- [OTP Pattern](#otp-pattern)
- [Session Pattern](#session-pattern)
- [Rate Limiting](#rate-limiting)
- [Best Practices](#best-practices)

---

## 🎯 Redis Basics

### Docker Configuration
```yaml
# docker-compose.yml (already configured)
redis:
  container_name: hotel_redis
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes --requirepass redis123
  volumes:
    - redis_data:/data
  networks:
    - hotel_network
  healthcheck:
    test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
  restart: unless-stopped
```

### Redis CLI Commands
```bash
# Connect to Redis
docker exec -it hotel_redis redis-cli -a redis123

# Basic commands
SET key "value"
GET key
DEL key
EXISTS key
EXPIRE key 300    # Expire in 300 seconds
TTL key          # Check remaining TTL
```

---

## 🎨 Caching Patterns

### 1. **Cache-Aside Pattern** (Most Common)

**How it works:**
```
1. Application checks Redis first
2. If MISS → Get from Database
3. Store in Redis for next time
4. Return to user
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RoomResponse getRoom(UUID id) {
        String cacheKey = "room:" + id;
        
        // 1. Check cache
        RoomResponse cached = (RoomResponse) redisTemplate
            .opsForValue()
            .get(cacheKey);
        
        if (cached != null) {
            log.info("Cache HIT for room: {}", id);
            return cached;
        }
        
        // 2. Cache MISS - Get from DB
        log.info("Cache MISS for room: {}", id);
        Room room = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Room not found"));
        
        RoomResponse response = mapToResponse(room);
        
        // 3. Store in cache (5 minutes)
        redisTemplate.opsForValue()
            .set(cacheKey, response, Duration.ofMinutes(5));
        
        return response;
    }
    
    public void updateRoom(UUID id, RoomRequest request) {
        // Update database
        Room room = repository.findById(id).orElseThrow();
        // ... update logic
        repository.save(room);
        
        // Invalidate cache
        String cacheKey = "room:" + id;
        redisTemplate.delete(cacheKey);
        log.info("Cache invalidated for room: {}", id);
    }
}
```

**Pros & Cons:**
| Pros | Cons |
|------|------|
| ✅ Simple to implement | ❌ Cache stampede risk |
| ✅ Cache only what's needed | ❌ Stale data possible |
| ✅ Fault tolerant (works without Redis) | ❌ Extra read on miss |

---

### 2. **Write-Through Pattern**

**How it works:**
```
1. Application writes to Cache first
2. Cache writes to Database
3. Return success
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // 1. Save to database
        Booking booking = mapToEntity(request);
        booking = repository.save(booking);
        
        BookingResponse response = mapToResponse(booking);
        
        // 2. Immediately write to cache
        String cacheKey = "booking:" + booking.getId();
        redisTemplate.opsForValue()
            .set(cacheKey, response, Duration.ofHours(1));
        
        // 3. Also add to user's bookings list
        String userBookings = "user:bookings:" + booking.getGuestId();
        redisTemplate.opsForList()
            .rightPush(userBookings, booking.getId().toString());
        redisTemplate.expire(userBookings, Duration.ofHours(24));
        
        return response;
    }
}
```

**Pros & Cons:**
| Pros | Cons |
|------|------|
| ✅ Always consistent | ❌ Extra write latency |
| ✅ No cache misses on new data | ❌ Wastes cache on infrequently accessed data |

---

### 3. **Write-Behind (Write-Back) Pattern**

**How it works:**
```
1. Write to Cache immediately
2. Return success
3. Async write to Database later
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class ViewCountService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomRepository repository;
    
    public void incrementViewCount(UUID roomId) {
        String key = "room:views:" + roomId;
        
        // Increment in Redis (fast)
        Long newCount = redisTemplate.opsForValue().increment(key);
        
        // Set expiry if first increment
        if (newCount == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(5));
        }
    }
    
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void syncViewCountsToDatabase() {
        Set<String> keys = redisTemplate.keys("room:views:*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        for (String key : keys) {
            UUID roomId = UUID.fromString(key.replace("room:views:", ""));
            Long views = (Long) redisTemplate.opsForValue().get(key);
            
            if (views != null && views > 0) {
                // Batch update to database
                repository.incrementViewCount(roomId, views.intValue());
                
                // Reset Redis counter
                redisTemplate.delete(key);
            }
        }
        
        log.info("Synced view counts for {} rooms", keys.size());
    }
}
```

**Pros & Cons:**
| Pros | Cons |
|------|------|
| ✅ Ultra-fast writes | ❌ Risk of data loss |
| ✅ Reduced DB load | ❌ Complex to implement |
| ✅ Good for analytics | ❌ Eventual consistency |

---

### 4. **Read-Through Pattern**

**How it works:**
```
Cache automatically loads from DB on miss
```

**Implementation with Spring Cache:**
```java
@Service
@CacheConfig(cacheNames = "rooms")
public class RoomService {
    
    @Cacheable(key = "#id")
    public RoomResponse getRoom(UUID id) {
        // Cache handles everything!
        return repository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new NotFoundException("Room not found"));
    }
    
    @CachePut(key = "#id")
    public RoomResponse updateRoom(UUID id, RoomRequest request) {
        // Updates cache automatically
        Room room = repository.findById(id).orElseThrow();
        // ... update logic
        return mapToResponse(repository.save(room));
    }
    
    @CacheEvict(key = "#id")
    public void deleteRoom(UUID id) {
        // Removes from cache
        repository.deleteById(id);
    }
}
```

**Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## Specific Patterns for Hotel Management

### Pattern 1: OTP Storage (Already Implemented)

**Use Case:** Email verification codes

**Redis Data Structure:**
```redis
# OTP value
SET otp:user@email.com "123456"
EXPIRE otp:user@email.com 300  # 5 minutes

# Attempts counter
SET otp:attempts:user@email.com 0
EXPIRE otp:attempts:user@email.com 300

# Resend cooldown
SET otp:cooldown:user@email.com 1732800000
EXPIRE otp:cooldown:user@email.com 60
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class OTPService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public String generateOTP(String email) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        
        // Store OTP
        String key = "otp:" + email;
        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
        
        // Reset attempts
        String attemptsKey = "otp:attempts:" + email;
        redisTemplate.opsForValue().set(attemptsKey, 0, 5, TimeUnit.MINUTES);
        
        return otp;
    }
    
    public boolean validateOTP(String email, String otp) {
        String key = "otp:" + email;
        String stored = (String) redisTemplate.opsForValue().get(key);
        
        if (stored == null) {
            throw new OTPExpiredException("OTP expired");
        }
        
        String attemptsKey = "otp:attempts:" + email;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        
        if (attempts != null && attempts >= 3) {
            redisTemplate.delete(key);
            throw new OTPAttemptsExceededException("Max attempts exceeded");
        }
        
        redisTemplate.opsForValue().increment(attemptsKey);
        
        boolean valid = stored.equals(otp);
        if (valid) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey);
        }
        
        return valid;
    }
}
```

---

### Pattern 2: Session Storage

**Use Case:** Store user sessions

**Redis Data Structure:**
```redis
# Session data
HSET session:abc123 userId "uuid-here"
HSET session:abc123 email "user@email.com"
HSET session:abc123 role "GUEST"
EXPIRE session:abc123 86400  # 24 hours
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "session:";
    
    public void createSession(String sessionId, UserInfo user) {
        String key = SESSION_PREFIX + sessionId;
        
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", user.getUserId());
        sessionData.put("email", user.getEmail());
        sessionData.put("role", user.getRole());
        sessionData.put("createdAt", System.currentTimeMillis());
        
        // Store as hash
        redisTemplate.opsForHash().putAll(key, sessionData);
        
        // Set expiration (24 hours)
        redisTemplate.expire(key, Duration.ofDays(1));
    }
    
    public UserInfo getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        
        if (data.isEmpty()) {
            return null;
        }
        
        return UserInfo.builder()
            .userId((String) data.get("userId"))
            .email((String) data.get("email"))
            .role((String) data.get("role"))
            .build();
    }
    
    public void extendSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.expire(key, Duration.ofDays(1));
    }
    
    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
```

---

### Pattern 3: Rate Limiting

**Use Case:** Prevent API abuse

**Redis Data Structure:**
```redis
# Fixed window
SET ratelimit:api:192.168.1.1 5
EXPIRE ratelimit:api:192.168.1.1 60  # 5 requests per minute
```

**Implementation:**
```java
@Component
@RequiredArgsConstructor
public class RateLimiterFilter implements Filter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        String ip = req.getRemoteAddr();
        String key = "ratelimit:api:" + ip;
        
        // Increment counter
        Long count = redisTemplate.opsForValue().increment(key);
        
        // Set expiry on first request
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }
        
        // Check limit
        if (count > MAX_REQUESTS) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429); // Too Many Requests
            res.getWriter().write("Rate limit exceeded. Try again later.");
            return;
        }
        
        // Add headers
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - count));
        
        chain.doFilter(request, response);
    }
}
```

---

### Pattern 4: Distributed Lock

**Use Case:** Prevent double-booking

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public BookingResponse createBooking(BookingRequest request) {
        String lockKey = "lock:room:" + request.getRoomId();
        
        // Acquire lock (expires in 10 seconds)
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));
        
        if (Boolean.FALSE.equals(acquired)) {
            throw new RoomNotAvailableException("Room is being booked by another user");
        }
        
        try {
            // Check availability
            if (!isRoomAvailable(request.getRoomId(), request.getCheckIn(), request.getCheckOut())) {
                throw new RoomNotAvailableException("Room not available for selected dates");
            }
            
            // Create booking
            Booking booking = createBookingInDB(request);
            
            return mapToResponse(booking);
            
        } finally {
            // Always release lock
            redisTemplate.delete(lockKey);
        }
    }
}
```

---

### Pattern 5: Leaderboard (Sorted Set)

**Use Case:** Most viewed rooms

**Redis Commands:**
```redis
# Increment view count
ZINCRBY room:leaderboard 1 "room-uuid-1"
ZINCRBY room:leaderboard 1 "room-uuid-2"

# Get top 10
ZREVRANGE room:leaderboard 0 9 WITHSCORES
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class RoomStatsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LEADERBOARD_KEY = "room:leaderboard";
    
    public void incrementViewCount(UUID roomId) {
        redisTemplate.opsForZSet()
            .incrementScore(LEADERBOARD_KEY, roomId.toString(), 1);
    }
    
    public List<RoomStats> getTopRooms(int limit) {
        Set<ZSetOperations.TypedTuple<Object>> top = redisTemplate.opsForZSet()
            .reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);
        
        if (top == null) {
            return Collections.emptyList();
        }
        
        return top.stream()
            .map(tuple -> new RoomStats(
                UUID.fromString((String) tuple.getValue()),
                tuple.getScore().longValue()
            ))
            .collect(Collectors.toList());
    }
}
```

---

## ⚡ Performance Optimization

### Pipeline (Batch Operations)
```java
public void batchUpdate(List<Room> rooms) {
    redisTemplate.executePipelined(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
            for (Room room : rooms) {
                String key = "room:" + room.getId();
                operations.opsForValue().set(key, room, Duration.ofMinutes(5));
            }
            return null;
        }
    });
}
```

### Lua Scripts (Atomic Operations)
```java
@Service
public class AtomicOTPService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String VALIDATE_SCRIPT = 
        "local otp = redis.call('get', KEYS[1]) " +
        "if otp == ARGV[1] then " +
        "  redis.call('del', KEYS[1]) " +
        "  return 1 " +
        "else " +
        "  return 0 " +
        "end";
    
    public boolean validateOTPAtomic(String email, String otp) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(VALIDATE_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(
            script,
            Collections.singletonList("otp:" + email),
            otp
        );
        
        return result != null && result == 1L;
    }
}
```

---

## 📊 Monitoring & Debugging

### Redis CLI Monitoring
```bash
# Monitor all commands
redis-cli -a redis123 MONITOR

# Get stats
redis-cli -a redis123 INFO stats
redis-cli -a redis123 INFO memory

# List all keys (DON'T use in production!)
redis-cli -a redis123 KEYS *

# Better: Scan with pattern
redis-cli -a redis123 SCAN 0 MATCH "room:*" COUNT 100
```

### Spring Boot Actuator
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,redis
  metrics:
    export:
      redis:
        enabled: true
```

---

## 🎯 Best Practices Summary

| Pattern | Use Case | TTL | Pros | Cons |
|---------|----------|-----|------|------|
| **Cache-Aside** | General caching | 5-60 min | Simple | Cache stampede |
| **Write-Through** | Critical data | Long | Consistent | Slower writes |
| **Write-Behind** | Analytics | Short | Fast writes | Data loss risk |
| **OTP Pattern** | Security codes | 5 min | Secure | Short-lived |
| **Session** | User sessions | 24 hours | Scalable | Memory intensive |
| **Rate Limit** | API protection | 1 min | Simple | Fixed window |
| **Distributed Lock** | Prevent conflicts | 10 sec | Atomic | Deadlock risk |

---

**✨ Redis Caching Patterns Complete!**
