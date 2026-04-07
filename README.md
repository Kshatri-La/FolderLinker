# 🔗 FileLinker (Dual-Site File Manager)
![JavaFX](https://img.shields.io/badge/JavaFX-13-orange.svg) ![JDK](https://img.shields.io/badge/JDK-11+-blue.svg) ![Build](https://img.shields.io/badge/Build-Maven-success.svg)

FileLinker is a specialized dual-pane file management utility explicitly designed to eliminate the friction of overwriting and migrating files between complex directory hierarchies. When working with "Old" and "New" project folders, FileLinker saves immense amounts of time by removing the need to manually dig through messy subdirectories to drag over files when root folders change structurally. 

> 🇻🇳 Cuộn xuống dưới để xem Hướng dẫn bằng Tiếng Việt.

---

## 🇺🇸 English Guide

### 🎯 Purpose & Philosophy
When maintaining two variations of a folder structure (e.g., v1 vs v2, Local vs Network, Original vs Modded), tracking down which nested files to overwrite becomes a chaotic waste of time. FileLinker is built to solve this by providing:
- **Instant Alignment:** Intelligently linking folders so the sender and receiver are always perfectly synced visually on the screen.
- **Precision Overwrites:** Letting you drop a nested file exactly where it belongs without manually traversing the destination's folder depths. 

### 🌟 Key Features
- **Dual-Pane Interface:** Two completely independent browser areas seamlessly divided, making bulk file migrations hyper-efficient.
- **🔗 Smart Directory Linking:** Click the link `🔗` button next to any folder. The system intelligently pins matching folders across both panes and color-codes them so you never lose your visual anchor.
- **Targeted Operations (Copy/Move/Overwrite):** Full suite of prompts preventing accidental data loss. Handles deep directory tree overrides elegantly inside a background thread for zero UI lag.
- **Smart Multi-File Routing:** Grab files from entirely separate directories and drop them onto the target pane—the system automatically identifies their roots and routes each individual file into its visually matching Linked Folder, avoiding grouped-folder clusters. The specific receiving folders visibly light up before you commit tracking.
- **Advanced Sorting Engine:** Sort by Name (Natural Numeric sorting e.g. File 2 before File 10), Size, or Date Modified simply by clicking the column headers.
- **Visual Drag & Drop Tracking:** Dragged items map precisely back to target row selections (`bôi đen` aesthetic). You retain unyielding clarity over what happens to your data upon release.

---

## 🇻🇳 Hướng Dẫn Chi Tiết (Tiếng Việt)

### 🎯 Mục Đích Cốt Lõi
Hệ thống **FileLinker** được sinh ra mang sứ mệnh giải quyết sự bế tắc khi phải **ghi đè hàng loạt file mới lên hệ thống file cũ** ở các thư mục sâu và phức tạp. 
Thay vì phải tốn thời gian mở hàng chục cửa sổ Windows lồng ghép để dò tìm thủ công, hoặc lạc lối khi "thư mục nền" (root folders) bị thay đổi cấu trúc, FileLinker giúp bạn:
- Tiết kiệm tuyệt đối thời gian tìm kiếm.
- Cố định và đồng bộ trực quan các thư mục ở cả 2 không gian làm việc.
- Kéo thả dán đè dữ liệu chính xác vào đúng thư mục nhanh chóng mà không gặp khó khăn khi chuyển đổi qua lại giữa file mới và file cũ.

### 🌟 Tính Năng Nổi Bật
- **Cơ Chế Liên Kết (Smart Link 🔗):** Khi bấm nút xích ở một thư mục, thuật toán sẽ tự động ghim chặt thư mục đó và thư mục tương ứng ở bên bảng đối diện lên đầu danh sách. Gắn mã màu rực rỡ để bạn luôn định vị được gốc tọa độ đang làm việc.
- **Kéo Thả Siêu Tốc & Chính Xác:** Hỗ trợ kéo thả file giữa hai không gian với điểm chỉ định rõ ràng. Khi bạn rà chuột mang file đi ngang qua, màu xanh rải thảm sẽ hiện lên báo hiệu đích xác thư mục nào sẽ nhận lệnh ghi đè.
- **Chế Độ Ghi Đè Thông Minh:** Kiểm tra xung đột file và cung cấp lựa chọn chi tiết (`Overwrite`, `Skip`, `Overwrite All`). Tách biệt tính toán chuyển file chạy ngầm dưới nền (Background Thread) nên không bao giờ bị đơ/lag khung hình.
- **Định Tuyến Đa File Phân Tán (Smart Auto-Routing):** Bạn có thể bôi đen nhiều file lộn xộn từ các khu vực khác nhau và ném thẳng sang màn đối diện. Hệ thống sẽ tự động đối chiếu gốc gác và "gửi trả" từng file về đúng tâm của Thư mục Link song sinh bên kia, xóa tan cảnh dồn cục file.
- **Radar Phát Hiện Bãi Đáp:** Thay vì hiển thị hộp thoại cảnh báo "mù", hệ thống sẽ tự động nhảy chuột dòng kẻ và bôi đen vùng thư mục đáp đất, kèm list tên file sẽ di chuyển để bạn kiểm chứng trước khi bấm OK.
- **Bộ Phân Loại Bậc Cao (Sorting):** Click thẳng vào tiêu đề các cột (Name, Size, Date) để tùy biến chiều ưu tiên. Hỗ trợ _Sắp xếp số học tự nhiên_ (ví dụ: `Bài 2` sẽ đứng chuẩn xác trước `Bài 10` chứ không xếp láo lộn xộn).

## Example

## Single

<img width="1225" height="416" alt="Ảnh chụp màn hình 2026-04-07 085051" src="https://github.com/user-attachments/assets/9c194902-5128-4ecf-95f1-3d7ca843d71c" />
<img width="1234" height="742" alt="Ảnh chụp màn hình 2026-04-07 085108" src="https://github.com/user-attachments/assets/9931b16d-bf4a-4f28-a8c7-390385e1c243" />
<img width="1233" height="655" alt="Ảnh chụp màn hình 2026-04-07 085120" src="https://github.com/user-attachments/assets/c97a875c-00ba-4b46-ad86-2aceb58c7d53" />
<img width="1233" height="741" alt="Ảnh chụp màn hình 2026-04-07 085136" src="https://github.com/user-attachments/assets/ffdb7af6-9279-4e8b-85c3-71b843aeda9c" />
<img width="1234" height="753" alt="Ảnh chụp màn hình 2026-04-07 085145" src="https://github.com/user-attachments/assets/37cc1910-1ed9-4a83-b691-44b3ac1629ac" />
<img width="1229" height="749" alt="Ảnh chụp màn hình 2026-04-07 085153" src="https://github.com/user-attachments/assets/14b4e3f7-0fa7-4500-96a1-3a75bccd0eaf" />

## Multi

<img width="1231" height="515" alt="Ảnh chụp màn hình 2026-04-07 091546" src="https://github.com/user-attachments/assets/2f6059fa-f352-4c41-b42c-46c0fb0c6ed9" />
<img width="1239" height="407" alt="Ảnh chụp màn hình 2026-04-07 091600" src="https://github.com/user-attachments/assets/d9c87b72-f113-4a43-9be8-bd5c5661e218" />
