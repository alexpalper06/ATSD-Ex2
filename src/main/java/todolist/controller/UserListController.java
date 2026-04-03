package todolist.controller;

import todolist.dto.UserPreviewData;
import todolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class UserListController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registered")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, HttpSession session) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserPreviewData> usersPage = usuarioService.findAllUsersPreview(pageable);

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalItems", usersPage.getTotalElements());
        model.addAttribute("hasNext", usersPage.hasNext());
        model.addAttribute("hasPrevious", usersPage.hasPrevious());

        return "listaUsuarios";
    }
}
