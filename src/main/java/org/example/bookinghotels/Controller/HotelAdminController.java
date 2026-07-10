package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
public class HotelAdminController {

    private final HotelRoomService hotelRoomService;

    public HotelAdminController(HotelRoomService hotelRoomService) {
        this.hotelRoomService = hotelRoomService;
    }

    // 1. READ & SHOW FORM EDIT
    @GetMapping("/admin/hotels")
    public String hotels(Model model, @RequestParam(value = "editId", required = false) Integer editId) {

        model.addAttribute("hotels", hotelRoomService.getAllHotels());
        model.addAttribute("mediaList", new ArrayList<>()); // Giả lập thư viện ảnh

        if (editId != null) {
            // Gọi đúng hàm getHotelById từ Service của bạn
            Hotels hotelToEdit = hotelRoomService.getHotelById(editId);
            model.addAttribute("editBranch", hotelToEdit);
        } else {
            model.addAttribute("editBranch", null);
        }

        return "html/hotels-html/hotels";
    }

    // 2. CREATE & UPDATE (HỢP NHẤT HÀM SAVE)
    @PostMapping("/admin/hotels/save")
    public String saveHotel(@ModelAttribute("editBranch") Hotels hotel, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra xem đối tượng có ID chưa để quyết định gọi hàm Create hay Update
            if (hotel.getId() == null) {
                // Gọi hàm tạo mới của bạn
                hotelRoomService.createHotel(hotel);
                redirectAttributes.addFlashAttribute("success", "Tạo mới chi nhánh thành công!");
            } else {
                // Gọi hàm cập nhật của bạn (truyền ID và đối tượng hotel)
                hotelRoomService.updateHotel(hotel.getId(), hotel);
                redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin chi nhánh thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý: " + e.getMessage());
        }
        return "redirect:/admin/hotels";
    }

    // 3. DELETE
    @PostMapping("/admin/hotels/{id}/delete")
    public String deleteHotel(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Gọi đúng hàm deleteHotel từ Service của bạn
            hotelRoomService.deleteHotel(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa chi nhánh: " + e.getMessage());
        }
        return "redirect:/admin/hotels";
    }
}