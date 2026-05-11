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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import todolist.authentication.ManagerUserSession;
import todolist.controller.exception.AccesoNoAutorizadoException;
import todolist.dto.EquipoData;
import todolist.dto.EquipoDetalleData;
import todolist.dto.UsuarioData;
import todolist.service.EquipoService;
import todolist.service.EquipoServiceException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        model.addAttribute("totalPages", teamsPage.getTotalPages());
        model.addAttribute("totalItems", teamsPage.getTotalElements());
        model.addAttribute("hasNext", teamsPage.hasNext());
        model.addAttribute("hasPrevious", teamsPage.hasPrevious());

        return "listaEquipos";
    }

    @GetMapping("/teams/{id}/members")
    public String listTeamMembers(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        checkRegisteredUser();

        EquipoData equipo = equipoService.recuperarEquipo(id);
        List<UsuarioData> membersList = equipoService.usuariosEquipo(id);

        // Manual pagination due to the type of the method usuariosEquipo (return List not Page)
        int totalElements = membersList.size();
        int totalPages = (totalElements == 0) ? 1 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<UsuarioData> membersPage = membersList.subList(fromIndex, toIndex);

        model.addAttribute("equipo", equipo);
        model.addAttribute("members", membersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalElements);
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("hasPrevious", page > 0);


        return "listarMiembrosEquipo";
    }

   
    @GetMapping("/teams/new")
    public String nuevoEquipo(Model model) {
        checkRegisteredUser();
        model.addAttribute("equipoData", new EquipoData());
        return "nuevoEquipo";
    }

    @PostMapping("/teams/new")
    public String crearEquipo(@ModelAttribute EquipoData equipoData,
                             Model model, RedirectAttributes flash,
                             HttpSession session) {
        checkRegisteredUser();
        Long idUsuario = managerUserSession.usuarioLogeado();
        
        try {
            equipoService.crearEquipoConUsuario(equipoData.getNombre(), idUsuario);
        } catch (EquipoServiceException e) {
            // Add the error message to the model to display it in the view
            model.addAttribute("error", e.getMessage());
            // Return the name of the template (replace with your actual template name, e.g., "formNuevoEquipo")
            return "nuevoEquipo"; 
        }

        return "redirect:/teams";
    }

    
    @GetMapping("/teams/{id}")
    public String detalleEquipo(@PathVariable Long id, Model model, HttpSession session) {
        checkRegisteredUser();

        Long idUsuario = managerUserSession.usuarioLogeado();
        //EquipoData equipo = equipoService.recuperarEquipo(id);
        EquipoDetalleData equipo = equipoService.recuperarEquipoDetalle(id);

        model.addAttribute("equipo", equipo);
        model.addAttribute("currentUserId", idUsuario);
        //model.addAttribute("isMember", usuarios.stream().anyMatch(u -> u.getId().equals(idUsuario)));
        model.addAttribute("isMember", equipoService.esUsuarioMiembro(idUsuario, id));
        return "detalleEquipo";
    }

    @PostMapping("/teams/{id}")
    public String gestionarMiembroEquipo(@PathVariable Long id) {
        checkRegisteredUser();
        Long idUsuario = managerUserSession.usuarioLogeado();

        List<UsuarioData> usuarios = equipoService.usuariosEquipo(id);
        //boolean isMember = usuarios.stream().anyMatch(u -> u.getId().equals(idUsuario));
        boolean isMember = equipoService.esUsuarioMiembro(idUsuario, id);

        if (isMember) {
            equipoService.eliminarUsuarioDeEquipo(id, idUsuario);
            try {
                equipoService.recuperarEquipo(id);
                return "redirect:/teams/" + id;
            } catch (Exception e) {
                // Team was deleted (e.g., last member left)
                return "redirect:/teams";
            }
        } else {
            equipoService.añadirUsuarioAEquipo(id, idUsuario);
            return "redirect:/teams/" + id;
        }
    }
}
