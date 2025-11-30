# Image Upload Documentation

Tài liệu về tính năng upload và quản lý ảnh phòng.

## Tổng quan

Hệ thống lưu trữ ảnh phòng trực tiếp trên server (local storage), không sử dụng cloud storage.

## Backend Implementation

### 1. File Storage Service

**File:** `FileStorageService.java`

**Chức năng:**
- Lưu file vào thư mục local
- Tạo URL để truy cập file
- Xóa file khi không cần
- Generate unique filename (UUID)

**Configuration:**
```yaml
file:
  upload-dir: uploads/room-images  # Thư mục lưu ảnh
  base-url: http://localhost:8080  # Base URL để tạo link
```

**Methods:**
```java
// Lưu file và trả về URL
String storeFile(MultipartFile file)

// Xóa file theo filename
void deleteFile(String filename)

// Extract filename từ URL
String extractFilename(String fileUrl)
```

### 2. API Endpoints

#### Upload Room Images

```
POST /api/v1/rooms/{roomId}/images
Content-Type: multipart/form-data

Form Data:
- files: MultipartFile[] (multiple files)

Response:
{
    "success": true,
    "message": "Images uploaded successfully",
    "data": [
        "http://localhost:8080/uploads/room-images/uuid1.jpg",
        "http://localhost:8080/uploads/room-images/uuid2.jpg"
    ]
}
```

**Validation:**
- File không được rỗng
- Content-Type phải là image/*
- Kích thước tối đa: 5MB/file
- Tổng request size: 25MB

**Logic:**
- Ảnh đầu tiên tự động được set làm primary
- Mỗi ảnh được lưu với UUID unique
- URL được tạo tự động

#### Delete Room Image

```
DELETE /api/v1/rooms/{roomId}/images/{imageId}

Response:
{
    "success": true,
    "message": "Image deleted successfully",
    "data": null
}
```

**Logic:**
- Xóa file từ storage
- Xóa record từ database
- Nếu xóa primary image, tự động set ảnh khác làm primary

#### Set Primary Image

```
PATCH /api/v1/rooms/{roomId}/images/{imageId}/primary

Response:
{
    "success": true,
    "message": "Primary image updated successfully",
    "data": null
}
```

**Logic:**
- Unset primary flag của ảnh hiện tại
- Set primary flag cho ảnh mới

### 3. Database Schema

**Table:** `room_images`

```sql
CREATE TABLE room_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id VARCHAR(255) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE INDEX idx_room_images_room_id ON room_images(room_id);
CREATE INDEX idx_room_images_is_primary ON room_images(is_primary);
```

### 4. File Storage Structure

```
project-root/
└── uploads/
    └── room-images/
        ├── uuid1.jpg
        ├── uuid2.png
        ├── uuid3.webp
        └── ...
```

### 5. Static File Serving

**Configuration:** `FileStorageConfig.java`

```java
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/room-images/**")
                .addResourceLocations("file:uploads/room-images/");
    }
}
```

**Access URL:**
```
http://localhost:8080/uploads/room-images/{filename}
```

## Frontend Integration

### 1. Upload Images

```typescript
// Service method
async uploadRoomImages(roomId: string, files: File[]): Promise<string[]> {
    const formData = new FormData();
    files.forEach(file => {
        formData.append('files', file);
    });

    const response = await apiClient.post<ApiResponse<string[]>>(
        `/api/v1/rooms/${roomId}/images`,
        formData,
        {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        }
    );

    return response.data.data;
}
```

### 2. Delete Image

```typescript
async deleteRoomImage(roomId: string, imageId: string): Promise<void> {
    await apiClient.delete(`/api/v1/rooms/${roomId}/images/${imageId}`);
}
```

### 3. Set Primary Image

```typescript
async setPrimaryImage(roomId: string, imageId: string): Promise<void> {
    await apiClient.patch(`/api/v1/rooms/${roomId}/images/${imageId}/primary`);
}
```

## Security Considerations

### 1. File Validation

**Backend:**
```java
// Check file type
if (!contentType.startsWith("image/")) {
    throw new BadRequestException("Only image files allowed");
}

// Check file size
if (file.getSize() > 5 * 1024 * 1024) {
    throw new BadRequestException("File too large");
}
```

**Frontend:**
```typescript
// Validate before upload
const MAX_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

if (!ALLOWED_TYPES.includes(file.type)) {
    throw new Error('Invalid file type');
}

if (file.size > MAX_SIZE) {
    throw new Error('File too large');
}
```

### 2. File Name Security

- Sử dụng UUID thay vì tên file gốc
- Tránh path traversal attacks
- Không expose tên file gốc

### 3. Access Control

- Chỉ ADMIN/STAFF có quyền upload/delete
- Public có thể xem ảnh
- Validate roomId ownership

## Error Handling

### Common Errors

1. **File Too Large:**
```json
{
    "success": false,
    "message": "File size must not exceed 5MB",
    "errorCode": "FILE_001"
}
```

2. **Invalid File Type:**
```json
{
    "success": false,
    "message": "Only image files are allowed",
    "errorCode": "FILE_002"
}
```

3. **Room Not Found:**
```json
{
    "success": false,
    "message": "Room not found",
    "errorCode": "ROOM_001"
}
```

4. **Image Not Found:**
```json
{
    "success": false,
    "message": "Room image not found",
    "errorCode": "ROOM_004"
}
```

## Performance Optimization

### 1. Image Optimization

**Recommendations:**
- Resize images before upload (frontend)
- Compress images (backend)
- Use WebP format for better compression
- Generate thumbnails for list views

### 2. Caching

**HTTP Headers:**
```java
@GetMapping("/uploads/room-images/**")
public ResponseEntity<Resource> serveFile() {
    return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
            .body(resource);
}
```

### 3. CDN Integration (Future)

- Move to CDN for better performance
- Keep local storage as backup
- Implement lazy loading

## Deployment Considerations

### 1. Production Setup

**Docker Volume:**
```yaml
volumes:
  - ./uploads:/app/uploads
```

**Environment Variables:**
```env
FILE_UPLOAD_DIR=/app/uploads/room-images
FILE_BASE_URL=https://your-domain.com
```

### 2. Backup Strategy

- Regular backup of uploads directory
- Database backup includes image URLs
- Sync strategy for multiple servers

### 3. Scaling

**Single Server:**
- Local storage works fine
- Simple and fast

**Multiple Servers:**
- Use shared storage (NFS, S3)
- Or implement CDN
- Sync between servers

## Testing

### 1. Unit Tests

```java
@Test
void testStoreFile() {
    MockMultipartFile file = new MockMultipartFile(
        "file", 
        "test.jpg", 
        "image/jpeg", 
        "test data".getBytes()
    );
    
    String url = fileStorageService.storeFile(file);
    assertNotNull(url);
    assertTrue(url.contains("uploads/room-images/"));
}
```

### 2. Integration Tests

```java
@Test
void testUploadRoomImages() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "files", 
        "test.jpg", 
        "image/jpeg", 
        "test".getBytes()
    );
    
    mockMvc.perform(multipart("/api/v1/rooms/{id}/images", roomId)
            .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
}
```

## Monitoring

### 1. Storage Usage

- Monitor disk space
- Alert when > 80% full
- Implement cleanup for old images

### 2. Upload Metrics

- Track upload success/failure rate
- Monitor upload duration
- Track file sizes

### 3. Logs

```java
log.info("Uploaded {} images for room {}", files.size(), roomId);
log.error("Failed to store file: {}", filename, exception);
```

## Future Enhancements

- Image compression on upload
- Thumbnail generation
- Multiple image sizes (small, medium, large)
- Image cropping/editing
- Watermark support
- Cloud storage integration (S3, Cloudinary)
- Image CDN
- Progressive image loading
- Image lazy loading
- WebP conversion
- EXIF data extraction
- Image validation (AI-based)

