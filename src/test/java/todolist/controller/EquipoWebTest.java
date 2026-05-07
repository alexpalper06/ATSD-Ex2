package todolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import todolist.authentication.ManagerUserSession;
import todolist.dto.EquipoData;
import todolist.dto.UsuarioData;
import todolist.service.EquipoService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    // Auxiliar methods to not repeat code in each test
    private EquipoData crearEquipo(Long id, String nombre) {
        EquipoData equipo = new EquipoData();
        equipo.setId(id);
        equipo.setNombre(nombre);

        return equipo;
    }

    private UsuarioData crearUsuario(Long id, String nombre, String email) {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }

    @Test
    public void testGuestAccessToTeamsIsForbidden() throws Exception {
        // Given
        // Not logged user, return null
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // When, Then
        mockMvc.perform(get("/teams")).andExpect(status().isForbidden());
    }

    @Test
    public void testLoggedInUserCanAccessTeams() throws Exception {
        // Given
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);

        // Service returns empty page
        Page<EquipoData> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(emptyPage);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogueado", 1L)).andExpect(status().isOk());
    }

    // /team content tests
    @Test
    public void testTeamsLinkAppearsInNavbar() throws Exception {
        // Given
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        Page<EquipoData> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(emptyPage);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogueado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Teams"), containsString("/teams"))));
    }

    @Test
    public void testTeamListShowsTeams() throws Exception {
        // Given
        Page<EquipoData> page = new PageImpl<>(Arrays.asList(
                crearEquipo(1L, "Proyecto AAA"),
                crearEquipo(2L, "Proyecto BBB")
                ),
                PageRequest.of(0, 10), 2);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(page);

        // When, Then
        mockMvc.perform(get("/teams")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proyecto AAA")))
                .andExpect(content().string(containsString("Proyecto BBB")));
    }

    @Test
    public void testTeamListIsAlphabetical() throws Exception {
        Page<EquipoData> page = new PageImpl<>(Arrays.asList(
                crearEquipo(1L, "Proyecto AAA"),
                crearEquipo(2L, "Proyecto BBB")
                ),
                PageRequest.of(0, 10), 2);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(page);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Proyecto AAA.*Proyecto BBB.*")));
    }

    @Test
    public void testTeamNameIsLinkToMembers() throws Exception {
        // Given
        Page<EquipoData> page = new PageImpl<>(Collections.singletonList(crearEquipo(1L, "Proyecto Alpha")),
                PageRequest.of(0, 10), 1);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(page);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Proyecto Alpha"), containsString("/teams/1/members")
                )));
    }

    @Test
    public void testEmptyTeamListShowsWarnig() throws Exception {
        // Given
        Page<EquipoData> page = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(page);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No teams found")));
    }

    @Test
    public void testTeamListModelAttributes() throws Exception {
        // Given
        Page<EquipoData> page = new PageImpl<>(Collections.singletonList(crearEquipo(1L, "Proyecto Alpha")),
                PageRequest.of(0, 10), 1);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(page);

        // When, Then
        mockMvc.perform(get("/teams").sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("teams", "currentPage", "totalPages", "totalItems", "hasNext", "hasPrevious"))
                .andExpect(model().attribute("teams", hasSize(1)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalItems", 1L))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("hasPrevious", false));
    }

    @Test
    public void testTeamListPaginationSecondPage() throws Exception {
        // Given
        List<EquipoData> secondPageEquipos = new ArrayList<>();
        for (int i = 11; i <= 15; i++) {
            secondPageEquipos.add(crearEquipo((long) i, "Proyecto " + i));
        }

        Page<EquipoData> secondPage = new PageImpl<>(secondPageEquipos, PageRequest.of(1, 10), 15);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.findAllTeamsPreview(any())).thenReturn(secondPage);

        // When, Then
        mockMvc.perform(get("/teams").param("page", "1").sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(content().string(containsString("Proyecto 11")))
                .andExpect(content().string(containsString("Proyecto 15")));
    }


    // Acces tests to /teams/{id}/members
    @Test
    public void testGuestAccessToMembersIsForbidden() throws Exception {
        // Given
        // No hay usuario logueado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // When, Then
        mockMvc.perform(get("/teams/1/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testLoggedInUserCanAccessMembers() throws Exception {
        // Given
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(crearEquipo(1L, "Proyecto Alpha"));
        when(equipoService.usuariosEquipo(1L)).thenReturn(new ArrayList<>());

        // When, Then
        mockMvc.perform(get("/teams/1/members")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk());
    }

    @Test
    public void testMembersPageShowsTeamName() throws Exception {
        // Given
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(crearEquipo(1L, "Proyecto Alpha"));
        when(equipoService.usuariosEquipo(1L)).thenReturn(new ArrayList<>());

        // When, Then
        // El nombre del equipo debe aparecer en el título de la página,
        // extraído de ${equipo.nombre} en listaMiembrosEquipo.html
        mockMvc.perform(get("/teams/1/members")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proyecto Alpha")));
    }

    @Test
    public void testMembersPageShowsMembers() throws Exception {
        // Given
        // Un equipo con dos miembros
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(crearEquipo(1L, "Proyecto Alpha"));
        when(equipoService.usuariosEquipo(1L)).thenReturn(Arrays.asList(
                crearUsuario(10L, "Alice", "alice@umh.es"),
                crearUsuario(11L, "Bob", "bob@umh.es")
        ));

        // When, Then
        // El HTML debe mostrar el nombre y email de cada miembro,
        // extraídos de ${member.nombre} y ${member.email} en la vista
        mockMvc.perform(get("/teams/1/members")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Alice"),
                        containsString("alice@umh.es"),
                        containsString("Bob"),
                        containsString("bob@umh.es")
                )));
    }

    @Test
    public void testEmptyMembersListShowsWarning() throws Exception {
        // GIVEN
        // Un equipo sin miembros
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(crearEquipo(1L, "Proyecto Vacío"));
        when(equipoService.usuariosEquipo(1L)).thenReturn(new ArrayList<>());

        // WHEN, THEN
        // La vista debe mostrar el mensaje de alerta definido en el bloque
        // th:if="${members == null or members.size() == 0}" de listaMiembrosEquipo.html
        mockMvc.perform(get("/teams/1/members")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No members found.")));
    }

    @Test
    public void testMembersPageModelAttributes() throws Exception {
        // GIVEN
        // Un equipo con un miembro
        EquipoData equipo = crearEquipo(1L, "Proyecto Alpha");
        UsuarioData usuario = crearUsuario(10L, "Alice", "alice@umh.es");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.singletonList(usuario));

        // WHEN, THEN
        // Verificamos los siete atributos que la vista espera del modelo.
        // "equipo" es especialmente importante porque se usa tanto para mostrar el nombre como para construir los enlaces de paginación con equipo.id en listaMiembrosEquipo.html
        mockMvc.perform(get("/teams/1/members")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(
                        "equipo", "members", "currentPage",
                        "totalPages", "totalItems", "hasNext", "hasPrevious"))
                .andExpect(model().attribute("equipo", equipo))
                .andExpect(model().attribute("members", hasSize(1)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalItems", 1))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", false));
    }

    @Test
    public void testMembersPaginationSecondPage() throws Exception {
        // GIVEN
        // Un equipo con 15 miembros. El controller hace paginación manual con subList, así que el mock devuelve los 15 miembros completos y el controller calcula qué subconjunto mostrar según page y size.
        List<UsuarioData> allMembers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            allMembers.add(crearUsuario((long) i, "User " + i, "user" + i + "@umh.es"));
        }

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(equipoService.recuperarEquipo(1L)).thenReturn(crearEquipo(1L, "Proyecto Grande"));
        when(equipoService.usuariosEquipo(1L)).thenReturn(allMembers);

        // WHEN, THEN
        // Con page=1 y size=10 por defecto, el controller calcula:
        // fromIndex=10, toIndex=15 -> muestra User 11 a User 15
        // hasPrevious=true porque page(1) > 0
        // hasNext=false porque page(1) == totalPages-1(1)
        mockMvc.perform(get("/teams/1/members")
                        .param("page", "1")
                        .sessionAttr("idUsuarioLogeado", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(content().string(containsString("User 11")))
                .andExpect(content().string(containsString("User 15")))
                // Verificamos que los emails de la primera página NO aparecen
                .andExpect(content().string(not(containsString("user1@umh.es"))))
                .andExpect(content().string(not(containsString("user10@umh.es"))));
    }
}
