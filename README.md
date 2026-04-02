This revised version of the **README.md** streamlines the documentation while retaining the essential technical implementation details and code snippets required for the ATSD P2 project.

-----

# To Do List App (Spring Boot & Thymeleaf)

A task management application built with **Spring Boot** and **Thymeleaf**.

## Getting Started

### Requirements

* **Java 21 SDK**
* Maven

### Execution

Run the application using Maven:

```bash
$ ./mvn spring-boot:run 
```

Or package and run as a JAR:

```bash
$ ./mvn package
$ java -jar target/todolist-inicial-0.0.1-SNAPSHOT.jar 
```

Access the app at: [http://localhost:8080/login](https://www.google.com/search?q=http://localhost:8080/login)

-----

## Implemented Functionalities

### Menu Bar

The application features a responsive navigation bar implemented as a reusable **Thymeleaf fragment**. It dynamically adapts based on the user's authentication state.

#### 1\. Session Management

The `ManagerUserSession` was enhanced to store the `username` globally, allowing the navbar to display the logged-in user's name.

```java
public void logearUsuario(Long idUsuario, String username) {
    session.setAttribute("idUsuarioLogeado", idUsuario);
    session.setAttribute("username", username);
}
```

#### 2\. Controller Integration

The `LoginController` passes user metadata to the session upon successful authentication:

```java
if (loginStatus == UsuarioService.LoginStatus.LOGIN_OK) {
    UsuarioData usuario = usuarioService.findByEmail(loginData.geteMail());
    managerUserSession.logearUsuario(usuario.getId(), usuario.getNombre());
    return "redirect:/usuarios/" + usuario.getId() + "/tareas";
}
```

#### 3\. Template Logic (`fragments.html`)

The navbar uses `th:if` logic to toggle visibility of links. Authenticated users see "Tasks" and a user dropdown, while unauthenticated users see "Login/Register."

```html
<nav th:fragment="navbar" class="navbar navbar-expand-lg navbar-light bg-light">
    <ul class="navbar-nav mr-auto">
        <li class="nav-item" th:if="${session.idUsuarioLogeado != null}">
            <a class="nav-link" th:href="@{/usuarios/{id}/tareas(id=${session.idUsuarioLogeado})}">Tasks</a>
        </li>
    </ul>
    <ul class="navbar-nav ml-auto">
        <li class="nav-item dropdown" th:if="${session.idUsuarioLogeado != null}">
            <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-toggle="dropdown">
                <span th:text="${session.username}">User</span>
            </a>
            <div class="dropdown-menu">
                <a class="dropdown-item text-danger" th:href="@{/logout}">Log out</a>
            </div>
        </li>
    </ul>
</nav>
```

-----

#### Testing

The implementation is verified through several MockMvc test layers. In order to check authenticated functionality, a session
manager mock is used on each test.
```java
when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
```
Tests were created in ther corresponding controllers in order to maintain modilarity.
* **`AboutPageTest`**: Checks that Username appears when logged in, and login and register does when not.
Mocks of session attributes `.sessionAttr` were used to check the behaviour.
```java
// Example of part of code used to check tha when the user is logged in, Account drop down menu appears instead of login and register

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
                not(containsString("href=\"/registro\">Register"),
                containsString("id=\"userDropdown\""),
                containsString(username)
...

```
* **`TareaWebTest`**: Ensures the "Tasks" link maintains the correct path (e.g., `/usuarios/1/tareas`), on listing, 
creation and modification.
```java
// Code snippet used to check the url maintains the correct path
...
String url = "/usuarios/" + usuarioId.toString() + "/tareas";

this.mockMvc.perform(get(url).sessionAttr("idUsuarioLogeado", usuarioId))
    .andExpect(status().isOk())
    .andExpect(content().string(
    // Tasks link should be present with correct user ID
        containsString("href=\"" + url + "\">Tasks")
    ));
...
```
* **`UsuarioWebTest`**: Confirms the navbar is excluded from Login/Registration.
```java
this.mockMvc.perform(get("/login"))
    .andExpect(content().string(allOf(
        // Navbar elements should not be present
        not(containsString("nav")),
        not(containsString("navbar"))
         
    )));
```

