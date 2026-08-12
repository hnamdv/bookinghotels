package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.service.ActiveHotelService;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
public class HotelAdminController {

    private final HotelRoomService hotelRoomService;
    private final ActiveHotelService activeHotelService;

    public HotelAdminController(
            HotelRoomService hotelRoomService,
            ActiveHotelService activeHotelService
    ) {
        this.hotelRoomService = hotelRoomService;
        this.activeHotelService = activeHotelService;
    }

    // =========================================================
    // 1. DANH SÁCH CHI NHÁNH + FORM THÊM / SỬA
    // =========================================================
    @GetMapping("/admin/hotels")
    public String hotels(
            Model model,
            @RequestParam(value = "editId", required = false) Integer editId
    ) {

        // Danh sách tất cả chi nhánh
        model.addAttribute(
                "hotels",
                hotelRoomService.getAllHotels()
        );

        // Thư viện ảnh
        model.addAttribute(
                "mediaList",
                new ArrayList<>()
        );

        // Chi nhánh đang active
        model.addAttribute(
                "activeHotelId",
                activeHotelService.getActiveHotelId()
        );

        // Nếu đang sửa chi nhánh
        if (editId != null) {

            Hotels hotelToEdit =
                    hotelRoomService.getHotelById(editId);

            model.addAttribute(
                    "editBranch",
                    hotelToEdit
            );

        } else {

            model.addAttribute(
                    "editBranch",
                    null
            );
        }

        return "html/hotels-html/hotels";
    }


    // =========================================================
    // 2. CHỌN CHI NHÁNH ĐANG HOẠT ĐỘNG
    // =========================================================
    @PostMapping("/admin/hotels/{id}/activate")
    public String activateHotel(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            // Lưu ID chi nhánh vào Session
            activeHotelService.setActiveHotel(id);

            // Lấy thông tin chi nhánh để thông báo
            Hotels hotel =
                    hotelRoomService.getHotelById(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã chuyển sang chi nhánh: " + hotel.getName()
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể chọn chi nhánh: " + e.getMessage()
            );
        }

        return "redirect:/admin/hotels";
    }


    // =========================================================
    // 3. CREATE + UPDATE
    // =========================================================
    @PostMapping("/admin/hotels/save")
    public String saveHotel(
            @ModelAttribute("editBranch") Hotels hotel,
            RedirectAttributes redirectAttributes
    ) {

        try {

            // Không có ID => tạo mới
            if (hotel.getId() == null) {

                hotelRoomService.createHotel(hotel);

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Tạo mới chi nhánh thành công!"
                );

            } else {

                // Có ID => cập nhật
                hotelRoomService.updateHotel(
                        hotel.getId(),
                        hotel
                );

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Cập nhật thông tin chi nhánh thành công!"
                );
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi xử lý: " + e.getMessage()
            );
        }

        return "redirect:/admin/hotels";
    }


    // =========================================================
    // 4. XÓA CHI NHÁNH
    // =========================================================
    @PostMapping("/admin/hotels/{id}/delete")
    public String deleteHotel(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            // Nếu đang xóa đúng chi nhánh active
            Integer activeHotelId =
                    activeHotelService.getActiveHotelId();

            if (activeHotelId != null &&
                    activeHotelId.equals(id)) {

                // Xóa activeHotelId khỏi Session
                activeHotelService.clearActiveHotel();
            }

            // Xóa chi nhánh
            hotelRoomService.deleteHotel(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã xóa chi nhánh thành công!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xóa chi nhánh: " + e.getMessage()
            );
        }

        return "redirect:/admin/hotels";
    }
}