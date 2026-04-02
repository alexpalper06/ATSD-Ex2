package todolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import todolist.authentication.ManagerUserSession;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class AboutPageTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerUserSession managerUserSession;

    @Test
    public void getAboutReturnsNameApplication() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("ToDoList")));
    }

    @Test
    public void navbarShowsCorrectLeftContent() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/about\">ToDoList"),
                        containsString("href=\"/tareas\">Tasks"),
                        containsString("href=\"/registered\">User List")
                )));
    }
    @Test
    public void navbarShowsLoginAndRegisterWhenNotLoggedIn() throws Exception {
        // GIVEN
        // No user is logged in (default state)

        // WHEN, THEN
        // The navbar should display Login and Register links
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/login\">Login"),
                        containsString("href=\"/registro\">Register"),
                        // Ensure dropdown menu is not shown
                        not(containsString("id=\"userDropdown\""))
                )));
    }

    @Test
    public void navbarShowsUserDropdownWhenLoggedIn() throws Exception {
        // GIVEN
        // A user is logged in
        Long userId = 1;
        String username = "Alex Palacios Perez";

        // Mock the managerUserSession to simulate a logged-in user
        when(managerUserSession.usuarioLogeado()).thenReturn(userId);

        // WHEN, THEN
        // The navbar should display username dropdown with Account and Logout options
        this.mockMvc.perform(get("/about")
                        .sessionAttr("idUsuarioLogeado", userId)
                        .sessionAttr("username", username))
                .andExpect(content().string(allOf(
                        // Ensure Login and Register are NOT shown
                        not(containsString("href=\"/login\">Login")),
                        not(containsString("href=\"/registro\">Register")),
                        // Ensure dropdown is shown with username
                        containsString("id=\"userDropdown\""),
                        containsString(username),
                        // Dropdown items
                        containsString("Account"),
                        containsString("href=\"/logout\">"),
                        containsString("Log out")
                )));
    }
}
