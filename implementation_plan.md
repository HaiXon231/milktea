# Kế hoạch Triển khai (Deploy) Casso Milktea Bot

Vì tính chất gấp rút để hoàn thành yêu cầu từ nhà tuyển dụng, cách nhanh nhất, miễn phí và ổn định nhất để deploy một dự án **Spring Boot + PostgreSQL + Telegram Bot** là sử dụng **Render (render.com)**.

## Mục tiêu
Triển khai Casso Milktea Bot lên Internet để Bot có thể hoạt động 24/7 và nhận webhook từ PayOS trên môi trường server thật.

> [!IMPORTANT]
> **Yêu cầu quan trọng trước khi bắt đầu:**
> Dự án của bạn phải được đẩy lên một kho lưu trữ (Repository) trên **GitHub** hoặc **GitLab** (có thể là public hoặc private). Render sẽ tự động lấy code từ đó để build thông qua `Dockerfile` đã có sẵn trong dự án.

---

### Bước 1: Khởi tạo Cơ sở dữ liệu PostgreSQL
1. Truy cập **[Render.com](https://render.com/)**, đăng nhập bằng GitHub.
2. Tại màn hình Dashboard, chọn **New +** -> **PostgreSQL**.
3. Điền các thông tin:
   - **Name**: `casso-db`
   - **Region**: Chọn `Singapore` (để gần Việt Nam, tốc độ mạng nhanh).
   - **Instance Type**: `Free`
4. Nhấn **Create Database**.
5. Sau khi tạo xong, cuộn xuống phần **Connections** và copy chuỗi **Internal Database URL** (dùng cho service chạy trên Render) và **External Database URL** (dùng nếu muốn kết nối từ máy local của bạn).

---

### Bước 2: Tạo Web Service cho Spring Boot
1. Trở lại Dashboard, chọn **New +** -> **Web Service**.
2. Kết nối với tài khoản GitHub của bạn và chọn Repository chứa dự án `Casso`.
3. Cấu hình Web Service:
   - **Name**: `casso-milktea-bot`
   - **Region**: `Singapore`
   - **Branch**: `main` (hoặc nhánh bạn đang lưu code)
   - **Environment**: Chọn `Docker` (Render sẽ tự động dùng file `Dockerfile` có sẵn).
   - **Instance Type**: `Free`

---

### Bước 3: Thiết lập Biến môi trường (Environment Variables)
Tại trang cấu hình Web Service, cuộn xuống phần **Environment Variables**, thêm các biến sau để ghi đè cấu hình trong `application.yml`:

| Key | Value (Copy chính xác từ code của bạn) |
|---|---|
| `SPRING_DATASOURCE_URL` | *(Lấy giá trị **Internal Database URL** từ bước 1, nhớ thêm `jdbc:` vào trước)* |
| `TELEGRAM_BOT_TOKEN` | `7957520350:AAHZxkoyfV4k5PDYBxboY67iwp843zY149I` |
| `TELEGRAM_BOT_USERNAME` | `home_milktea_bot` |
| `OPENAI_API_KEY` | `AIzaSyAsfGZuvpsmoXCkyz5ZYjNFyDpeL13IDhE` |
| `PAYOS_CLIENT_ID` | `55643e9b-11c3-4f66-8970-7fc1abdd1f35` |
| `PAYOS_API_KEY` | `efd86d9a-c341-44b9-95b6-b903a445e7fd` |
| `PAYOS_CHECKSUM_KEY` | `c02fb6c782dceb1df35c1e192117ff008eda1f03105290ed57142bc4688b3d87` |
| `APP_BASE_URL` | *(Để trống lúc này, sau khi deploy thành công thì copy URL của Render dán vào đây)* |
| `AI_MODE` | `openai` |

> [!TIP]
> **Cách điền Database URL chuẩn cho Spring Boot:**
> Render sẽ cung cấp URL dạng: `postgres://user:password@hostname/dbname`.
> Bạn cần đổi chữ `postgres://` thành `jdbc:postgresql://` để Spring Boot hiểu được.

---

### Bước 4: Deploy và Cấu hình Webhook PayOS
1. Nhấn **Create Web Service** và chờ Render tự động build Docker container. Quá trình này mất khoảng 5-10 phút.
2. Khi deploy thành công, Render sẽ cung cấp cho bạn một đường dẫn (URL) dạng: `https://casso-milktea-bot.onrender.com`.
3. Bạn quay lại file thiết lập Environment Variables trên Render, cập nhật giá trị của `APP_BASE_URL` thành `https://casso-milktea-bot.onrender.com`.
4. Mở **Dashboard của PayOS**, cài đặt Webhook URL trỏ tới `https://casso-milktea-bot.onrender.com/payos/webhook`. (Đây là bước rất quan trọng để nhận thông báo thanh toán thành công!).

---

## Verification Plan

### Manual Verification
- **Test kết nối Telegram:** Lên Telegram gõ `/start` với bot xem bot có phản hồi không.
- **Test chức năng:** Đặt một ly trà sữa và lấy mã QR. Thanh toán mã QR đó.
- **Test Webhook:** Chờ khoảng 5-10 giây để xem bot có tự động báo "Thanh toán thành công" vào trong nhóm Telegram hay không (điều này chứng minh Webhook từ PayOS đã đâm ngược lại server Render thành công).
