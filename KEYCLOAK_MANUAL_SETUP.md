# Hướng dẫn Cấu hình Keycloak Thủ công (Manual Setup)

Tài liệu này hướng dẫn bạn cấu hình Keycloak từng bước thông qua giao diện Admin Console thay vì sử dụng dòng lệnh (CLI).

## 1. Truy cập Admin Console

1.  Mở trình duyệt và truy cập: `http://localhost:8180` (hoặc port bạn đã cấu hình).
2.  Nhấn vào **Administration Console**.
3.  Đăng nhập với tài khoản admin (mặc định thường là `admin` / `admin123` nếu bạn dùng file docker-compose cũ).

---

## 2. Tạo Realm (Vùng quản lý)

Realm là không gian quản lý riêng biệt cho ứng dụng của bạn.

1.  Ở góc trên bên trái, di chuột vào chữ **Master** (hoặc tên realm hiện tại).
2.  Nhấn nút **Create Realm**.
3.  Nhập thông tin:
    *   **Realm name**: `hotel-realm`
4.  Nhấn **Create**.

---

## 3. Cấu hình User Registration (Đăng ký người dùng)

Để cho phép người dùng tự đăng ký tài khoản:

1.  Đảm bảo bạn đang ở `hotel-realm`.
2.  Vào menu **Realm Settings** (bên trái).
3.  Chọn tab **Login**.
4.  Bật các tùy chọn sau:
    *   **User registration**: `ON` (Cho phép đăng ký).
    *   **Forgot password**: `ON` (Cho phép quên mật khẩu).
    *   **Remember me**: `ON` (Cho phép "Nhớ đăng nhập").
    *   **Verify email**: `ON` (Yêu cầu xác thực email - *Khuyên dùng*).
    *   **Login with email**: `ON` (Cho phép đăng nhập bằng email thay vì username).
5.  Nhấn **Save**.

---

## 4. Cấu hình Email (SMTP)

Để Keycloak gửi được email xác thực hoặc quên mật khẩu, bạn cần cấu hình SMTP.

1.  Vào menu **Realm Settings**.
2.  Chọn tab **Email**.
3.  Nhập thông tin (Ví dụ dùng Gmail):
    *   **Host**: `smtp.gmail.com`
    *   **Port**: `587`
    *   **From**: `noreply@hotel.com` (Hoặc email của bạn).
    *   **Enable Authentication**: `ON`.
    *   **Username**: Email Gmail của bạn.
    *   **Password**: [App Password](https://myaccount.google.com/apppasswords) của Gmail (Không phải mật khẩu đăng nhập thường).
    *   **Enable StartTLS**: `ON`.
4.  Nhấn **Save**.
5.  Nhấn nút **Test connection** để kiểm tra xem gửi mail có thành công không.

---

## 5. Tạo Client (Ứng dụng)

Client đại diện cho ứng dụng Frontend (React/Vite) của bạn.

1.  Vào menu **Clients** (bên trái).
2.  Nhấn **Create client**.
3.  **Bước 1: General Settings**:
    *   **Client type**: `OpenID Connect`
    *   **Client ID**: `hotel-app`
    *   **Name**: `Hotel Management App`
    *   Nhấn **Next**.
4.  **Bước 2: Capability config**:
    *   **Client authentication**: `OFF` (Vì đây là Public Client - SPA).
    *   **Authentication flow**:
        *   Standard flow: `ON`
        *   Direct access grants: `ON`
    *   Nhấn **Next**.
5.  **Bước 3: Login settings**:
    *   **Root URL**: `http://localhost:3000` (Frontend port mới của bạn).
    *   **Home URL**: `http://localhost:3000`
    *   **Valid redirect URIs**:
        *   `http://localhost:3000/*`
        *   `http://localhost:5173/*` (Dự phòng nếu dùng port cũ).
    *   **Web origins**:
        *   `+` (Cho phép tất cả từ redirect URIs) hoặc nhập cụ thể `http://localhost:3000`.
6.  Nhấn **Save**.

---

## 6. Cấu hình Social Login (Google & Facebook)

### 6.1. Google Login

**Bước 1: Lấy Credentials từ Google**
1.  Truy cập [Google Cloud Console](https://console.cloud.google.com/).
2.  Tạo Project mới.
3.  Vào **APIs & Services** > **Credentials**.
4.  Tạo **OAuth Client ID**.
5.  Chọn Application type: **Web application**.
6.  Thêm **Authorized redirect URI**:
    *   Copy link từ Keycloak: Vào Keycloak > **Identity Providers** > **Google** > Copy link ở dòng "Redirect URI".
    *   Dạng: `http://localhost:8180/realms/hotel-realm/broker/google/endpoint`
7.  Lấy **Client ID** và **Client Secret**.

**Bước 2: Cấu hình trong Keycloak**
1.  Vào menu **Identity Providers** (bên trái).
2.  Chọn **Google**.
3.  Nhập thông tin:
    *   **Client ID**: (Dán Client ID từ Google).
    *   **Client Secret**: (Dán Client Secret từ Google).
4.  Nhấn **Add**.

### 6.2. Facebook Login

**Bước 1: Lấy Credentials từ Facebook**
1.  Truy cập [Meta for Developers](https://developers.facebook.com/).
2.  Tạo App mới > Chọn loại **Consumer** hoặc **Business**.
3.  Thêm sản phẩm **Facebook Login**.
4.  Vào **Facebook Login** > **Settings**.
5.  Thêm **Valid OAuth Redirect URIs**:
    *   Dạng: `http://localhost:8180/realms/hotel-realm/broker/facebook/endpoint`
6.  Vào **App Settings** > **Basic** để lấy **App ID** và **App Secret**.

**Bước 2: Cấu hình trong Keycloak**
1.  Vào menu **Identity Providers**.
2.  Chọn **Facebook**.
3.  Nhập thông tin:
    *   **Client ID**: (Dán App ID từ Facebook).
    *   **Client Secret**: (Dán App Secret từ Facebook).
4.  Nhấn **Add**.

---

## 7. Tạo Roles (Vai trò)

1.  Vào menu **Realm roles**.
2.  Nhấn **Create role**.
3.  Tạo lần lượt các role:
    *   `ADMIN`
    *   `STAFF`
    *   `GUEST`
4.  Nhấn **Save** sau mỗi lần tạo.

---

## 8. Tạo User Admin (Thử nghiệm)

1.  Vào menu **Users**.
2.  Nhấn **Add user**.
3.  Nhập:
    *   **Username**: `admin`
    *   **Email**: `admin@hotel.com`
    *   **First name**: `Admin`
    *   **Email verified**: `Yes` (Gạt sang ON).
4.  Nhấn **Create**.
5.  Chuyển sang tab **Credentials**.
6.  Nhấn **Set password**.
    *   Password: `admin123`
    *   Temporary: `OFF` (Tắt để không phải đổi pass lần đầu).
    *   Nhấn **Save**.
7.  Chuyển sang tab **Role mapping**.
8.  Nhấn **Assign role**.
9.  Chọn `ADMIN` và nhấn **Assign**.

---

## ✅ Hoàn tất

Bây giờ bạn đã có:
1.  Realm `hotel-realm`.
2.  Client `hotel-app` kết nối với Frontend port 3000.
3.  Tính năng Đăng ký user đang bật.
4.  Cấu hình gửi Email (SMTP).
5.  Tích hợp đăng nhập Google/Facebook (nếu đã nhập đúng ID/Secret).
