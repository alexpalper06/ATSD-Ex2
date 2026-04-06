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

-----

### User List

This functionality provides a paginated user directory at **`GET /registered`** for any user. It uses a lightweight DTO (`UserPreviewData`) so only the needed fields are exposed.

#### 1. DTO

`UserPreviewData` contains:
* `Long id` (Primary key of DB)
* `String nombre` (user name)
* `String email` (user email)

This class is used in the service and controller layer to transfer user preview data to the UI.

#### 2. Service Layer

A method in `UsuarioService` has been added, `findAllUsersPreview(Pageable pageable)`, with `@Transactional(readOnly = true)` and which entities via `ModelMapper`. It uses the class `Page` from Spring to allow pagination:

```java
public Page<UserPreviewData> findAllUsersPreview(Pageable pageable) {
    Page<Usuario> usuarios = usuarioRepository.findAll(pageable);
    return usuarios.map(usuario -> modelMapper.map(usuario, UserPreviewData.class));
}
```

#### 3. Controller Layer

`UserListController` handles the request and writes pagination model attributes. Uses `page` and `size` number in order to get information by chunks. To enable pagination, pagination state must be added to the model context.

```java
@GetMapping("/registered")
public String listUsers(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
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
```

#### 4. View (`listaUsuarios.html`)

The template includes `fragments::navbar` and `fragments::head`, iterates `th:each="user : ${users}"`, and shows a warning when users are empty.

```html
<tr th:each="user : ${users}">
    <td th:text="${user.id}"></td>
    <td th:text="${user.nombre}"></td>
    <td th:text="${user.email}"></td>
</tr>
<tr th:if="${#lists.isEmpty(users)}">
    <td colspan="3">No users found</td>
</tr>
```

Pagination controls use Bootstrap buttons and `th:classappend` for active/disabled states.

#### Testing

`UserListWebTest` validates:
* `/registered` renders `User List` and navbar content.
* pagination navigation delivers users 11-20 on page 2.
* model attributes (`users`, `currentPage`, `totalPages`, `totalItems`, `hasNext`, `hasPrevious`) are present.

```java
this.mockMvc.perform(get("/registered").param("page", "1"))
    .andExpect(status().isOk())
    .andExpect(content().string(allOf(
        containsString("usu11"),
        containsString("usu20"),
        not(containsString("usu1"))
    )));
```

`UsuarioServiceTest` validates:
* `testFindNoUser` empty pages are handled.
* `testFindSingleUser` returns exactly one user DTO with correct fields.
* `testFindAllLessThanPageLimit` returns all users when count < page size.
* `testFindAllExactPageLimit` returns the page-sized number of users.
* `testFindAllExceedPageLimit` applies pagination and returns only the requested page subset.

```java
Pageable pageable = PageRequest.of(0, 10);
Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);
assertThat(result).isNotNull();
assertThat(result.getContent()).hasSize(1);
assertThat(result.getTotalElements()).isEqualTo(1);
UserPreviewData user = result.getContent().get(0);
assertThat(user.getEmail()).isEqualTo("richard@umh.es");
```

-----

### User Description

This functionality allows users to view the complete profile of a registered account through a detail page accessible from the User List. It uses a specialized DTO (`UserDetailData`) to expose only the necessary information.

#### 1. DTO

`UserDetailData` contains:
* `Long id` (Primary key of DB)
* `String nombre` (user full name)
* `String email` (user email)
* `Date fechaNacimiento` (date of birth)

#### 2. Service Layer

A method in `UsuarioService` has been added, `findDetailsById(Long usuarioId)`, for retrieving the user information and mapping it to `UserDetailData` via `ModelMapper`:

```java
@Transactional(readOnly = true)
public UserDetailData findDetailsById(Long usuarioId) {
    Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
    if (usuario == null) return null;
    else {
        return modelMapper.map(usuario, UserDetailData.class);
    }
}
```

#### 3. Controller Layer

`UserListController` handles the detail page request at **`GET /registered/{id}`**. The controller extracts the user ID from the path variable, calls the service to fetch the DTO, and adds it to the model:

```java
@GetMapping("/registered/{id}")
public String viewUserDetails(@PathVariable Long id, Model model) {
    UserDetailData user = usuarioService.findDetailsById(id);
    model.addAttribute("user", user);
    return "detalleUsuario";
}
```

#### 4. View (`detalleUsuario.html`)

The template includes `fragments::navbar` and `fragments::head`, displays user details in a Bootstrap card layout, and provides error handling for non-existent users:

```html
<div th:if="${user != null}">
    <div class="card">
        <div class="card-body">
            <div class="list-group list-group-flush">
                <div class="list-group-item">
                    <strong>Id:</strong>
                    <span th:text="${user.id}"></span>
                </div>
                <div class="list-group-item">
                    <strong>Full Name:</strong>
                    <span th:text="${user.nombre}"></span>
                </div>
                <div class="list-group-item">
                    <strong>Email:</strong>
                    <span th:text="${user.email}"></span>
                </div>
                <div class="list-group-item">
                    <strong>Date of Birth:</strong>
                    <span th:text="${#dates.format(user.fechaNacimiento, 'dd/MM/yyyy')}"></span>
                </div>
            </div>
        </div>
    </div>
    <a class="btn btn-primary" th:href="@{/registered}">Back to List</a>
</div>

<div th:if="${user == null}" class="alert alert-danger" role="alert">
    <strong>User not found.</strong> The requested user does not exist in the system.
</div>
```

To allow the user navigate to a specific user's profile, the User List page (`listaUsuarios.html`) was updated to link user names to the detail page:

```html
<td class="text-truncate">
    <a th:href="@{/registered/{id}(id=${user.id})}" th:text="${user.nombre}"></a>
</td>
```

#### Testing

`UserListWebTest` validates:
* `/registered/1` renders user details correctly when user exists.
* `/registered/999` shows error message when user does not exist.
* Model attribute `user` is present and populated correctly.
* Links in the User List page navigate to `/registered/{id}` endpoints.

```java
@Test
public void testViewUserDetailsFound() throws Exception {
    UserDetailData userDetails = new UserDetailData();
    userDetails.setId(1L);
    userDetails.setNombre("John Doe");
    userDetails.setEmail("john@example.com");

    when(usuarioService.findDetailsById(1L)).thenReturn(userDetails);

    this.mockMvc.perform(get("/registered/1"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("user"))
        .andExpect(content().string(allOf(
            containsString("John Doe"),
            containsString("john@example.com")
        )));
}
```

`UsuarioServiceTest` validates:
* `testFindDetailsById` returns the complete user detail when user exists.
* `testFindDetailsByIdUserNotFound` returns null when user does not exist.
* All fields (`id`, `nombre`, `email`, `fechaNacimiento`) are correctly mapped from the entity.

```java
@Test
public void testFindDetailsById() {
    Long usuarioId = addUsuarioBD();

    UserDetailData usuarioDetalle = usuarioService.findDetailsById(usuarioId);

    assertThat(usuarioDetalle).isNotNull();
    assertThat(usuarioDetalle.getId()).isEqualTo(usuarioId);
    assertThat(usuarioDetalle.getNombre()).isEqualTo("Richard Stallman");
    assertThat(usuarioDetalle.getEmail()).isEqualTo("richard@umh.es");
}
```



