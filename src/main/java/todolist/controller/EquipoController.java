package todolist.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import todolist.authentication.ManagerUserSession;
import todolist.controller.exception.AccesoNoAutorizadoException;
import todolist.dto.EquipoData;
import todolist.dto.UsuarioData;
import todolist.service.EquipoService;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private void checkRegisteredUser() {
        if (managerUserSession.usuarioLogeado() == null) {
            throw new AccesoNoAutorizadoException();
        }
    }

    @GetMapping("/teams")
    public String listTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, HttpSession session) {

        checkRegisteredUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());

        Page<EquipoData> teamsPage = equipoService.findAllTeamsPreview(pageable);

        model.addAttribute("teams", teamsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", teamsPage.getTotalPages());
        model.addAttribute("hasNext", teamsPage.hasNext());
        model.addAttribute("hasPrevious", teamsPage.hasPrevious());

        return "listaEquipos";
    }

    @GetMapping("/teams/{id}/members")
    public String listTeamMembers(@PathVariable Long id, Model model) {
        checkRegisteredUser();

        EquipoData equipo = equipoService.recuperarEquipo(id);

        List<UsuarioData> membersList = equipoService.usuariosEquipo(id);

        model.addAttribute("equipo", equipo);
        model.addAttribute("members", membersList);

        return "listaMiembrosEquipo";
    }
}
