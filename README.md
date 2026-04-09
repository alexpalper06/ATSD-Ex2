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

### Links

You may access the source code and an image for running the application on:
* [GitHub](https://github.com/alexpalper06/ATSD-Ex2)
* [DockerHub](https://hub.docker.com/r/alexpalper06/p2-todolist)

Use the following links to access the main functionalities of the app:
* Access the login at [http://localhost:8080/login](http://localhost:8080/login)
* Register an account at [http://localhost:8080/registro](http://localhost:8080/registro)
* Check information about the application at [http://localhost:8080/about](http://localhost:8080/about)
* **Admin only**. Check list of users at [http://localhost:8080/registered](http://localhost:8080/registered)
* **Login required**. Check your tasks at [http://localhost:8080/usuarios/{id}/tareas](http://localhost:8080/usuarios/{id}/tareas)
-----

## Implemented Functionalities

### Menu Bar

The application features a responsive navigation bar implemented as a reusable **Thymeleaf fragment**. It dynamically adapts based on the user's authentication state.

#### 1\. Session Management

The `ManagerUserSession` was enhanced to store the `username` globally, allowing the navbar to display the logged in user's name.

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

// Mock the managerUserSession to simulate a logged in user
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
// ...

```
* **`TareaWebTest`**: Ensures the "Tasks" link maintains the correct path (e.g., `/usuarios/1/tareas`), on listing, 
creation and modification.
```java
// Code snippet used to check the url maintains the correct path
// ...
String url = "/usuarios/" + usuarioId.toString() + "/tareas";

this.mockMvc.perform(get(url).sessionAttr("idUsuarioLogeado", usuarioId))
    .andExpect(status().isOk())
    .andExpect(content().string(
    // Tasks link should be present with correct user ID
        containsString("href=\"" + url + "\">Tasks")
    ));
// ...
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

This functionality provides a paginated user directory at **`GET /registered`** accessible only to admin users. It uses a lightweight DTO (`UserPreviewData`) so only the needed fields are exposed.

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
    checkAdminAccess(); // More information on the functionality User List & Description Protection

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

This functionality allows admin users to view the complete profile of a registered account through a detail page accessible from the User List. It uses a specialized DTO (`UserDetailData`) to expose only the necessary information.

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

-----

### Admin User

This functionality is provided by implementing a simple rol-based system. This could have been implemented with SpringSecurity library for Role-based authentication, but given limited time the other system was developed.

#### 1. Role Enum

A new enum `UsuarioRol` was created with two values:

```java
public enum UsuarioRol {
    USER,
    ADMIN
}
```

#### 2. Entity Changes

The `Usuario` entity was assigned a role field with a default value of `USER`:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private UsuarioRol rol = UsuarioRol.USER;

// Ensure default role if null at persistence
@PrePersist
private void ensureRol() {
    if (rol == null) rol = UsuarioRol.USER;
}
```

#### 3. Repository Layer

A method `existsByRol()` was added to `UsuarioRepository` to check if an admin user exists to be used by the service:

```java
public interface UsuarioRepository extends PagingAndSortingRepository<Usuario, Long>, CrudRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String s);
    boolean existsByRol(UsuarioRol rol);
}
```

#### 4. Service Layer

The `UsuarioService` includes:

- **`adminExists()`**: Checks if an admin user exists in the system using the repository method.

```java
@Transactional(readOnly = true)
public boolean adminExists() {
    return usuarioRepository.existsByRol(UsuarioRol.ADMIN);
}
```

- **`registrar()`**: Modified to prevent registration of multiple admins on backend level:

```java
@Transactional
public UsuarioData registrar(UsuarioData usuario) {
    Optional<Usuario> usuarioBD = usuarioRepository.findByEmail(usuario.getEmail());
    // ...
    else if (usuario.getRol() == UsuarioRol.ADMIN && adminExists())
        throw new UsuarioServiceException("Ya existe un administrador en el sistema");
    //...
}
```

#### 5. Session Management

The `ManagerUserSession` class was modified to store user rol information:

```java
public void logearUsuario(Long idUsuario, String username, UsuarioRol rol) {
    session.setAttribute("idUsuarioLogeado", idUsuario);
    session.setAttribute("username", username);
    session.setAttribute("rolUsuarioLogeado", rol);
}
```

#### 6. Controller Logic

The `LoginController` was modified to:

- **Login and Register**: When the user log ins or  register they're redirected to their respecitve sections; `/registered` for admin users and task list for regular users. This was allowed by refactoring authentication logic to an external method.

```java
private String performLogin(String email, String password, Model model) {
        UsuarioService.LoginStatus loginStatus = usuarioService.login(email, password);

        if (loginStatus != UsuarioService.LoginStatus.LOGIN_OK) {
            model.addAttribute("loginData", new LoginData(email, password));
        }

        if (loginStatus == UsuarioService.LoginStatus.LOGIN_OK) {
            UsuarioData usuario = usuarioService.findByEmail(email);

            managerUserSession.logearUsuario(usuario.getId(), usuario.getNombre(), usuario.getRol());

            if (usuario.getRol() == UsuarioRol.ADMIN) {
                return "redirect:/registered";
            }
            return "redirect:/usuarios/" + usuario.getId() + "/tareas";
        } else if (loginStatus == UsuarioService.LoginStatus.USER_NOT_FOUND) {
            model.addAttribute("error", "No existe usuario");
            return "formLogin";
        } else if (loginStatus == UsuarioService.LoginStatus.ERROR_PASSWORD) {
            model.addAttribute("error", "Contraseña incorrecta");
            return "formLogin";
        }
        return "formLogin";
    }
```
- **On Login**: Modified to use the previous method.
```java
@PostMapping("/login")
public String loginSubmit(@ModelAttribute LoginData loginData, Model model, HttpSession session) {
    // Llamada al servicio para comprobar si el login es correcto
    return performLogin(loginData.geteMail(), loginData.getPassword(), model);
}
```

- **On Registration**: Display the admin checkbox only if no admin exists by setting up an attribute, and set the role accordingly. Logs in automatically for improved user experience.

```java
@GetMapping("/registro")
public String registroForm(Model model) {
    model.addAttribute("registroData", new RegistroData());
    model.addAttribute("adminExists", usuarioService.adminExists());
    return "formRegistro";
}

@PostMapping("/registro")
public String registroSubmit(@Valid RegistroData registroData, BindingResult result, Model model) {
    // validation and variable instantiation

    // Set rol to ADMIN if checkbox is selected
    if (registroData.isAdmin()) {
        usuario.setRol(UsuarioRol.ADMIN);
    } else {
        usuario.setRol(UsuarioRol.USER);
    }

    usuarioService.registrar(usuario);
    return performLogin(usuario.getEmail(), usuario.getPassword(), model);
}
```

#### 7. DTOs

The `UsuarioData` DTO includes the role field:

```java
private UsuarioRol rol;

public UsuarioRol getRol() {
    return rol;
}

public void setRol(UsuarioRol rol) {
    this.rol = rol;
}
```

The `RegistroData` DTO includes an admin flag with a default value of `false`. This is necessary for the registration form checkbox:

```java
private Boolean isAdmin = false;

public boolean isAdmin() {
    return isAdmin;
}

public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
}
```

#### 8. Registration Form (`formRegistro.html`)

The registration form conditionally displays an admin checkbox only when no admin exists:

```html
<div class="form-group" th:if="${!adminExists}">
    <div class="form-check">
        <input class="form-check-input" type="checkbox" id="isAdmin" name="isAdmin" th:field="*{admin}" />
        <label class="form-check-label" for="isAdmin">
            Registrar como administrador
        </label>
    </div>
</div>
```

#### 9. Testing

Comprehensive test coverage was implemented in both service and controller layers:

**Service Layer (`UsuarioServiceTest`)**:

- **`testAdminExistsFalse`**: Verifies that `adminExists()` returns false when no admin is in the database.

- **`testAdminExistsTrue`**: Verifies that `adminExists()` returns true when an admin user exists.

- **`testDefaultRoleIsUser`**: Confirms that users registered without specifying a role default to `USER`.

```java
@Test
public void testDefaultRoleIsUser() {
    UsuarioData usuario = new UsuarioData();
    usuario.setEmail("usuario.prueba@gmail.com");
    usuario.setPassword("12345678");
    usuario.setNombre("Prueba User");

    UsuarioData usuarioRegistrado = usuarioService.registrar(usuario);

    assertThat(usuarioRegistrado.getRol()).isEqualTo(UsuarioRol.USER);
}
```

**Controller Layer (`UsuarioWebTest`)**:

- **`adminCheckboxVisibleWhenNoAdminExists`**: Verifies the admin checkbox appears on the registration form when no admin exists.

```java
@Test
public void adminCheckboxVisibleWhenNoAdminExists() throws Exception {
    when(usuarioService.adminExists()).thenReturn(false);
    
    this.mockMvc.perform(get("/registro"))
        .andExpect(content().string(containsString("Registrar como administrador")));
}
```

- **`adminCheckboxHiddenWhenAdminExists`**: Verifies the admin checkbox is hidden after an admin is registered.

```java
@Test
public void adminCheckboxHiddenWhenAdminExists() throws Exception {
    when(usuarioService.adminExists()).thenReturn(true);
    
    this.mockMvc.perform(get("/registro"))
        .andExpect(content().string(not(containsString("Registrar como administrador"))));
}
```

- **`testAdminLoginRedirect`**: Verifies that admin users are redirected to `/registered` after login.

```java
@Test
public void testAdminLoginRedirect() throws Exception {
    // ... user setup

    when(usuarioService.login("admin@gmail.com", "12345678"))
        .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
    when(usuarioService.findByEmail("admin@gmail.com"))
        .thenReturn(adminUser);

    this.mockMvc.perform(post("/login")
            .param("eMail", "admin@gmail.com")
            .param("password", "12345678"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/registered"));
}
```


-----

### User List and Description Protection

This functionality implements authorization checks to ensure that only admin users can access the user list and user profile pages. Non-admin users attempting to access these endpoints receive an unauthorized access exception.

#### 1. Authorization Exception

A custom exception `AccesoNoAutorizadoException` was created to handle unauthorized access attempts:

```java
@ResponseStatus(value = HttpStatus.FORBIDDEN, reason="Acceso no autorizado a usuarios estandar")
public class AccesoNoAutorizadoException extends RuntimeException {
}
```

#### 2. Session Management Enhancement

The `ManagerUserSession` class includes an `isAdmin()` method to check if the logged in user has admin privileges:

```java
public boolean isAdmin() {
    UsuarioRol rol = (UsuarioRol) session.getAttribute("rolUsuarioLogeado");
    return rol != null && rol == UsuarioRol.ADMIN;
}
```

#### 3. Controller Layer Protection

The `UserListController` was modified to include authorization checks before processing requests. A `checkAdminAccess()` method verifies admin status and throws an exception if unauthorized:

```java
private void checkAdminAccess() {
    if (!managerUserSession.isAdmin()) {
        throw new AccesoNoAutorizadoException();
    }
}

@GetMapping("/registered")
public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Model model, HttpSession session) {

    checkAdminAccess();
    // ... rest of method
}

@GetMapping("/registered/{id}")
public String viewUserDetails(@PathVariable Long id, Model model) {
    checkAdminAccess();
    
    UserDetailData user = usuarioService.findDetailsById(id);
    model.addAttribute("user", user);
    return "detalleUsuario";
}
```

#### 4. Navbar Link Protection

The navigation bar template was updated to conditionally display the "User List" link only for authenticated admin users using Thymeleaf conditionals:

```html
<li class="nav-item" th:if="${session.idUsuarioLogeado != null && session.isAdmin}">
    <a class="nav-link" th:href="@{/registered}">User List</a>
</li>
```

#### 5. Testing
Tests were implmented to check the authorization worked. Navigation bar related test was forked into different situations given the status of the user:

**Controller Layer `UserListWebTest`**:

- **`testForbiddenUserAccess` and `testForbiddenGuestAccess`**: Verifies that non-admin users receive an unauthorized response when attempting to access `/registered`.

```java
@Test
public void testForbiddenUserAccess() throws Exception {
    // GIVEN
    // A standard user is logged in (not admin)  
    // Mock the service to return an empty page
    Page<UserPreviewData> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
    when(usuarioService.findAllUsersPreview(any())).thenReturn(emptyPage);
    when(managerUserSession.isAdmin()).thenReturn(false);

    // WHEN
    // We make a GET request to /registered with standard user role

    // THEN
    // The response should be Forbidden
    this.mockMvc.perform(get("/registered")
                    .sessionAttr("idUsuarioLogeado", 1L)
                    .sessionAttr("username", "standardUser")
                    .sessionAttr("rolUsuarioLogeado", UsuarioRol.USER))
            .andExpect(status().isForbidden());   
}

- **`testSuccessAdminAccess`**: Verifies that admin users can access `/registered`.
public void testSuccessAdminAccess() throws Exception {
    //...

    this.mockMvc.perform(get("/registered")
                    .sessionAttr("idUsuarioLogeado", 1L)
                    .sessionAttr("username", "adminUser")
                    .sessionAttr("rolUsuarioLogeado", UsuarioRol.ADMIN))
            .andExpect(status().isOk());
}
```

- **`testUserListInNavAsAdmin`**: Verifies the "User List" link is shown in the navbar for admin users.
- **`testUserListInNavAsUser` and `testUserListInNavAsGuest`**: Verifies the "User List" link is hidden in the navbar for non-admin users.

```java
@Test
public void testUserListInNavAsAdmin() throws Exception {
  
when(managerUserSession.isAdmin()).thenReturn(true);

this.mockMvc.perform(get("/registered")
                .sessionAttr("idUsuarioLogeado", 1L)
                .sessionAttr("username", "adminUser")
                .sessionAttr("rolUsuarioLogeado", UsuarioRol.ADMIN))
        .andExpect(status().isOk())
        .andExpect(content().string(allOf(
                containsString("User List"),
                containsString("navbar"),
                containsString("/registered")
        )));
}

@Test
public void testUserListInNavAsUser() throws Exception {
    this.mockMvc.perform(get("/about")
            .sessionAttr("idUsuarioLogeado", 2L)
            .sessionAttr("username", "standardUser")
            .sessionAttr("rolUsuarioLogeado", UsuarioRol.USER))
    .andExpect(content().string(not(containsString("User List"))))
    .andExpect(content().string(not(containsString("/registered"))));
}
```

