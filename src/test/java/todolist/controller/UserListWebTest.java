package todolist.controller;

import todolist.authentication.ManagerUserSession;
import todolist.dto.UsuarioData;
import todolist.dto.UserPreviewData;
import todolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserListWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    @Test
    public void testUserListInNavbar() throws Exception {
       
        // Mock the service to return an empty page
        Page<UserPreviewData> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(usuarioService.findAllUsersPreview(any())).thenReturn(emptyPage);

        // WHEN
        // We make a GET request to /registered

        // THEN
        // The resulting HTML contains the "User List" link within the navbar structure
        this.mockMvc.perform(get("/registered"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("User List"),
                        containsString("navbar"),
                        containsString("/registered")
                )));
    }

    @Test
    public void testPaginationNavigation() throws Exception {
        // GIVEN
        // 25 users mocked (with a default page size of 10, we have multiple pages)
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);

        List<UserPreviewData> page1Users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserPreviewData user = new UserPreviewData();
            user.setId((long) (i + 1));
            user.setNombre("usu" + (i + 1));
            user.setEmail("usu" + (i + 1) + "@gmail.com");
            page1Users.add(user);
        }

        List<UserPreviewData> page2Users = new ArrayList<>();
        for (int i = 10; i < 20; i++) {
            UserPreviewData user = new UserPreviewData();
            user.setId((long) (i + 1));
            user.setNombre("usu" + (i + 1));
            user.setEmail("usu" + (i + 1) + "@gmail.com");
            page2Users.add(user);
        }

        Page<UserPreviewData> firstPage = new PageImpl<>(page1Users, PageRequest.of(0, 10), 25);
        Page<UserPreviewData> secondPage = new PageImpl<>(page2Users, PageRequest.of(1, 10), 25);

        when(usuarioService.findAllUsersPreview(PageRequest.of(0, 10)))
                .thenReturn(firstPage);
        when(usuarioService.findAllUsersPreview(PageRequest.of(1, 10)))
                .thenReturn(secondPage);

        // WHEN
        // We request page 2 (/registered?page=1)

        // THEN
        // The correct subset of users (users 11-20) is returned
        this.mockMvc.perform(get("/registered").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<td class=\"text-truncate\">usu11</td>"),
                        containsString("<td class=\"text-truncate\">usu20</td>"),
                        not(containsString("<td class=\"text-truncate\">1</td>")),
                        not(containsString("<td class=\"text-truncate\">10</td>"))
                )));
    }

    @Test
    public void testPaginatedDataInModel() throws Exception {
        // GIVEN
        // The service returns a specific page of UserPreviewDTOs
        when(managerUserSession.usuarioLogeado()).thenReturn(1L);

        List<UserPreviewData> users = Arrays.asList(
                new UserPreviewData(1L, "Alice", "alice@example.com"),
                new UserPreviewData(2L, "Bob", "bob@example.com"),
                new UserPreviewData(3L, "Charlie", "charlie@example.com")
        );

        Page<UserPreviewData> page = new PageImpl<>(users, PageRequest.of(0, 10), 3);
        when(usuarioService.findAllUsersPreview(any())).thenReturn(page);

        // WHEN
        // We make a GET request to /registered

        // THEN
        // The controller correctly adds the Page object to the Model
        // and the view renders the expected number of user rows
        this.mockMvc.perform(get("/registered"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users", "currentPage", "totalPages", "totalItems", "hasNext", "hasPrevious"))
                .andExpect(model().attribute("users", hasSize(3)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalItems", 3L))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(content().string(allOf(
                        containsString("Alice"),
                        containsString("alice@example.com"),
                        containsString("Bob"),
                        containsString("bob@example.com"),
                        containsString("Charlie"),
                        containsString("charlie@example.com")
                )));
    }
}
