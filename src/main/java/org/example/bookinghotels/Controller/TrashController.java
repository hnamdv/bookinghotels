package org.example.bookinghotels.Controller;

import org.example.bookinghotels.service.TrashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/trash")
public class TrashController {

    @Autowired
    private TrashService trashService;

    @GetMapping
    public String viewTrash(Model model) {
        model.addAttribute("deletedInvoices", trashService.getDeletedInvoices());
        model.addAttribute("deletedUsers", trashService.getDeletedUsers());
        return "html/admin-html/trash";
    }

    @PostMapping("/restore/invoice/{id}")
    public String restoreInvoice(@PathVariable Integer id) {
        trashService.restoreInvoice(id);
        return "redirect:/admin/trash";
    }

    @PostMapping("/restore/user/{id}")
    public String restoreUser(@PathVariable Integer id) {
        trashService.restoreUser(id);
        return "redirect:/admin/trash";
    }
}