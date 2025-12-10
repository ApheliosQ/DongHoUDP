<h2 align="center"> <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin"> 🎓 Faculty of Information Technology (DaiNam University) </a> </h2> <h2 align="center"> ĐỒNG HỒ SERVER - CLIENT </h2> <div align="center"> <p align="center"> <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/> <img src="docs/fitdnu_logo.png" alt="FIT DNU Logo" width="180"/> <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/> </p>






</div>

Xin chào! 👋
Mình là sinh viên Khoa Công Nghệ Thông Tin – Đại học Đại Nam. Đây là dự án Đồng Hồ Server – Client được phát triển nhằm giúp sinh viên hiểu rõ cách giao tiếp mạng giữa các thiết bị bằng giao thức UDP (User Datagram Protocol).

# 📖 1. Giới thiệu hệ thống
🎯 Mục tiêu dự án

Dự án được xây dựng nhằm đạt được những mục tiêu sau:

✔️ Hiểu rõ kiến trúc Client – Server trong mô phỏng đồng hồ mạng.

✔️ Làm quen với lập trình socket trên Java sử dụng UDP Datagram.

✔️ Thực hành đồng bộ thời gian từ Server → Client theo thời gian thực.

✔️ Cài đặt thêm các chức năng mở rộng như báo thức, hẹn giờ, bấm giờ.

✔️ Xây dựng nền tảng để phát triển các ứng dụng giao tiếp thời gian thực trong IoT, mạng LAN hoặc các hệ thống nhúng.

⚙️ Cách hoạt động của hệ thống

Hệ thống gồm hai thành phần chính:

## 🖥️ Server UDP

Lắng nghe gói tin từ Client trên cổng SERVER_PORT.

Khi nhận yêu cầu, Server sẽ:

Lấy thời gian hệ thống hiện tại.

Đóng gói và gửi trả lại Client thông qua UDP Datagram.

Hoạt động liên tục, đồng thời hỗ trợ nhiều Client nhờ triển khai đa luồng.

## 💻 Client UDP

Gửi yêu cầu lên Server theo chu kỳ (ví dụ mỗi 1 giây).

Nhận thời gian trả về từ Server.

Cập nhật đồng hồ hiển thị trên giao diện.

Cung cấp các chức năng:

Đồng hồ thời gian thực (Real-Time Clock)

Báo thức (Alarm)

Hẹn giờ (Timer)

Bấm giờ (Stopwatch)

# 🔧 2. Ngôn ngữ lập trình và công nghệ sử dụng

🔤 Ngôn ngữ & Công cụ

🟦 Java 8+ – ngôn ngữ chính, hỗ trợ mạnh về lập trình mạng.

🟩 Eclipse/IntelliJ IDEA – IDE khuyến nghị để biên dịch & chạy.

🟧 JDK – cần cấu hình sẵn JAVA_HOME.

## 🛠️ Công nghệ chính
1. UDP Protocol (User Datagram Protocol)

Truyền dữ liệu không cần thiết lập kết nối.

Tốc độ nhanh, tài nguyên thấp.

Phù hợp cho ứng dụng cần cập nhật liên tục như đồng hồ, cảm biến IoT.

Không đảm bảo dữ liệu đến nơi → phải xử lý lỗi từ phía Client.

2. Client/Server Architecture

Server chạy nền và phục vụ thời gian cho nhiều Client.

Client độc lập, gửi request khi cần.

Kiến trúc này giúp:

Tách biệt xử lý thời gian.

Đồng bộ hóa thời gian theo 1 chuẩn chung.

3. Multithreading

Server triển khai Thread cho mỗi Client để tránh tắc nghẽn.

Giúp hệ thống mượt mà khi số lượng Client tăng cao.

4. Java I/O & DatagramSocket

Sử dụng DatagramSocket, DatagramPacket để gửi và nhận dữ liệu.

Giao tiếp 2 chiều giữa Server và Client.

# 🚀 3. Các chức năng chính và hình ảnh
## 🧩 Bảng mô tả chức năng
Phần mềm	Chức năng chính
Server UDP	- Lắng nghe yêu cầu từ Client
- Gửi thời gian hiện tại
- Hỗ trợ nhiều Client
Client UDP	- Nhận và hiển thị thời gian thực
- Đặt báo thức, hẹn giờ, bấm giờ
## 🖼️ Giao diện minh họa
<div align="center"> <img src="https://github.com/user-attachments/assets/7f97e10e-ae55-402d-868c-6371a0564017" width="70%"> <p><b>Hình 1:</b> Giao diện chính đồng hồ nhận thời gian từ Server</p> <img src="https://github.com/user-attachments/assets/1c24e41c-e141-4d7f-a251-4724bf61ccbc" width="70%"> <p><b>Hình 2:</b> Giao diện đặt báo thức với thời gian chính xác</p> <img src="https://github.com/user-attachments/assets/645e87d6-cb65-4acb-b273-39ee77d7d088" width="70%"> <p><b>Hình 3:</b> Giao diện hẹn giờ (Timer)</p> <img src="https://github.com/user-attachments/assets/9a74c375-4a08-469c-a1c9-a5996a41bcb2" width="70%"> <p><b>Hình 4:</b> Chức năng bấm giờ (Stopwatch) phục vụ thể thao/thí nghiệm</p> </div>

# 📝 4. Hướng dẫn cài đặt & chạy dự án trên Eclipse
## 1️⃣ Chuẩn bị môi trường

Bạn cần cài đặt đầy đủ:

✔️ Java JDK 8 hoặc cao hơn

✔️ Eclipse IDE hoặc IntelliJ

✔️ Git để clone dự án

Clone dự án:

git clone https://github.com/ApheliosQ/DongHoUDP.git

## 2️⃣ Chạy Server

Mở Eclipse →
File → Import → Existing Projects into Workspace

Chọn thư mục dự án.

Mở file:
udpclock/TimeServer.java

Kiểm tra cấu hình:

SERVER_PORT = 12345 (có thể thay đổi)

Chạy chương trình:
Run As → Java Application

Server sẽ hiển thị trạng thái:

Server is running...
Listening on port 12345

## 3️⃣ Chạy Client

Mở file:
udpclock/TimeClient.java

Chỉnh thông tin:

SERVER_ADDRESS = "127.0.0.1"; 
SERVER_PORT = 12345;


Chạy chương trình:
Run As → Java Application

Client hiển thị đồng hồ và kết nối tới Server.

# 📬 5. Liên hệ

💻 Người thực hiện: <span style="color:#1E90FF"><b>Đỗ Hoàng Quý</b></span>

✉️ Email: <a href="mailto:hquy2422004@gmail.com"><span style="color:#FF4500"><b>hquy2422004@gmail.com
</b></span></a>

📞 Số điện thoại: <span style="color:#32CD32"><b>0364225004</b></span>

© 2025 AIoTLab – Faculty of Information Technology – DaiNam University.
All rights reserved.
