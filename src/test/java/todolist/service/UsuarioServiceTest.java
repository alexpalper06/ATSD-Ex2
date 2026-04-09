package todolist.service;

import todolist.dto.UsuarioData;
import todolist.dto.UserDetailData;
import todolist.dto.UserPreviewData;
import todolist.model.UsuarioRol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve el identificador del usuario de la BD
    Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("richard@umh.es");
        usuario.setNombre("Richard Stallman");
        usuario.setRol(UsuarioRol.USER);
        usuario.setPassword("1234");
        UsuarioData nuevoUsuario = usuarioService.registrar(usuario);
        return nuevoUsuario.getId();
    }

    @Test
    public void servicioLoginUsuario() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // WHEN
        // intentamos logear un usuario y contraseña correctos
        UsuarioService.LoginStatus loginStatus1 = usuarioService.login("richard@umh.es", "1234");

        // intentamos logear un usuario correcto, con una contraseña incorrecta
        UsuarioService.LoginStatus loginStatus2 = usuarioService.login("richard@umh.es", "0000");

        // intentamos logear un usuario que no existe,
        UsuarioService.LoginStatus loginStatus3 = usuarioService.login("ricardo.perez@gmail.com", "12345678");

        // THEN

        // el valor devuelto por el primer login es LOGIN_OK,
        assertThat(loginStatus1).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);

        // el valor devuelto por el segundo login es ERROR_PASSWORD,
        assertThat(loginStatus2).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // y el valor devuelto por el tercer login es USER_NOT_FOUND.
        assertThat(loginStatus3).isEqualTo(UsuarioService.LoginStatus.USER_NOT_FOUND);
    }

    @Test
    public void servicioRegistroUsuario() {
        // WHEN
        // Registramos un usuario con un e-mail no existente en la base de datos,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba2@gmail.com");
        usuario.setPassword("12345678");

        usuarioService.registrar(usuario);

        // THEN
        // el usuario se añade correctamente al sistema.

        UsuarioData usuarioBaseDatos = usuarioService.findByEmail("usuario.prueba2@gmail.com");
        assertThat(usuarioBaseDatos).isNotNull();
        assertThat(usuarioBaseDatos.getEmail()).isEqualTo("usuario.prueba2@gmail.com");
    }

    @Test
    public void servicioRegistroUsuarioExcepcionConNullPassword() {
        // WHEN, THEN
        // Si intentamos registrar un usuario con un password null,
        // se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }


    @Test
    public void servicioRegistroUsuarioExcepcionConEmailRepetido() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // THEN
        // Si registramos un usuario con un e-mail ya existente en la base de datos,
        // , se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("richard@umh.es");
        usuario.setPassword("12345678");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }

    @Test
    public void servicioRegistroUsuarioDevuelveUsuarioConId() {

        // WHEN
        // Si registramos en el sistema un usuario con un e-mail no existente en la base de datos,
        // y un password no nulo,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");
        usuario.setPassword("12345678");

        UsuarioData usuarioNuevo = usuarioService.registrar(usuario);

        // THEN
        // se actualiza el identificador del usuario

        assertThat(usuarioNuevo.getId()).isNotNull();

        // con el identificador que se ha guardado en la BD.

        UsuarioData usuarioBD = usuarioService.findById(usuarioNuevo.getId());
        assertThat(usuarioBD).isEqualTo(usuarioNuevo);
    }

    @Test
    public void servicioConsultaUsuarioDevuelveUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos un usuario usando su e-mail,

        UsuarioData usuario = usuarioService.findByEmail("richard@umh.es");

        // THEN
        // el usuario obtenido es el correcto.

        assertThat(usuario.getId()).isEqualTo(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("richard@umh.es");
        assertThat(usuario.getNombre()).isEqualTo("Richard Stallman");
    }

    @Test
    public void testFindNoUser() {
        // GIVEN
        // No users in the database

        // WHEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);

        // THEN
        // The service returns an empty page, not an error
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void testFindSingleUser() {
        // GIVEN
        // A single user in the database
        Long usuarioId = addUsuarioBD();

        // WHEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);

        // THEN
        // The service returns a page with that user
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        UserPreviewData user = result.getContent().get(0);
        assertThat(user.getId()).isEqualTo(usuarioId);
        assertThat(user.getNombre()).isEqualTo("Richard Stallman");
        assertThat(user.getEmail()).isEqualTo("richard@umh.es");
    }

    @Test
    public void testFindAllLessThanPageLimit() {
        // GIVEN
        // 3 users in the database with a page size of 10
        for (int i = 0; i < 3; i++) {
            UsuarioData usuario = new UsuarioData();
            usuario.setEmail("user" + i + "@example.com");
            usuario.setNombre("User " + i);
            usuario.setPassword("password" + i);
            usuarioService.registrar(usuario);
        }

        // WHEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);

        // THEN
        // All users are returned in a single page
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    public void testFindAllExactPageLimit() {
        // GIVEN
        // Exactly 10 users in the database with a page size of 10
        for (int i = 0; i < 10; i++) {
            UsuarioData usuario = new UsuarioData();
            usuario.setEmail("email" + i + "@gmail.com");
            usuario.setNombre("usu " + i);
            usuario.setPassword("password123" + i);
            usuarioService.registrar(usuario);
        }

        // WHEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);

        // THEN
        // Exactly 10 users are returned
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    public void testFindAllExceedPageLimit() {
        // GIVEN
        // 15 users in the database with a page size of 10 (exceeding the limit)
        for (int i = 0; i < 15; i++) {
            UsuarioData usuario = new UsuarioData();
            usuario.setEmail("usu" + i + "@gmail.com");
            usuario.setNombre("usu" + i);
            usuario.setPassword("passwd123" + i);
            usuarioService.registrar(usuario);
        }

        // WHEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPreviewData> result = usuarioService.findAllUsersPreview(pageable);

        // THEN
        // Only 10 users are returned (the pagination limit)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    public void testFindDetailsById() {
        // GIVEN
        // A user in the database
        Long usuarioId = addUsuarioBD();

        // WHEN
        UserDetailData usuarioDetalle = usuarioService.findDetailsById(usuarioId);

        // THEN
        // The service returns the user detail correctly
        assertThat(usuarioDetalle).isNotNull();
        assertThat(usuarioDetalle.getId()).isEqualTo(usuarioId);
        assertThat(usuarioDetalle.getNombre()).isEqualTo("Richard Stallman");
        assertThat(usuarioDetalle.getEmail()).isEqualTo("richard@umh.es");
        assertThat(usuarioDetalle.getFechaNacimiento()).isNull();
    }

    @Test
    public void testFindDetailsByIdUserNotFound() {
        // WHEN
        // We search for a user that doesn't exist
        UserDetailData usuarioDetalle = usuarioService.findDetailsById(999L);

        // THEN
        // The service returns null
        assertThat(usuarioDetalle).isNull();
    }

    @Test
    public void testAdminExistsFalse() {
        // GIVEN
        // No admin exists in the database

        // WHEN
        boolean adminExists = usuarioService.adminExists();

        // THEN
        // The method returns false
        assertThat(adminExists).isFalse();
    }

    @Test
    public void testAdminExistsTrue() {
        // GIVEN
        // An admin user exists in the database
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@umh.es");
        admin.setNombre("Admin User");
        admin.setPassword("1234");
        admin.setRol(UsuarioRol.ADMIN);
        usuarioService.registrar(admin);

        // WHEN
        boolean adminExists = usuarioService.adminExists();

        // THEN
        // The method returns true
        assertThat(adminExists).isTrue();
    }

    @Test
    public void testDefaultRoleIsUser() {
        // WHEN
        // We register a user without specifying a role
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");
        usuario.setPassword("12345678");
        usuario.setNombre("Prueba User");

        UsuarioData usuarioRegistrado = usuarioService.registrar(usuario);

        // THEN
        // The user should have the role USER by default
        assertThat(usuarioRegistrado.getRol()).isEqualTo(todolist.model.UsuarioRol.USER);

        // Verify in database
        UsuarioData usuarioBD = usuarioService.findByEmail("usuario.prueba@gmail.com");
        assertThat(usuarioBD.getRol()).isEqualTo(todolist.model.UsuarioRol.USER);
    }

    @Test
    public void testRegisterAsAdmin() {
        // WHEN
        // We register an admin user
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("admin.prueba@gmail.com");
        usuario.setPassword("12345678");
        usuario.setNombre("Admin Prueba");
        usuario.setRol(todolist.model.UsuarioRol.ADMIN);

        UsuarioData usuarioRegistrado = usuarioService.registrar(usuario);

        // THEN
        // The user should have the ADMIN role
        assertThat(usuarioRegistrado.getRol()).isEqualTo(todolist.model.UsuarioRol.ADMIN);

        // Verify in database
        UsuarioData usuarioBD = usuarioService.findByEmail("admin.prueba@gmail.com");
        assertThat(usuarioBD.getRol()).isEqualTo(todolist.model.UsuarioRol.ADMIN);
    }

    @Test
    public void testCannotRegisterMultipleAdmins() {
        // GIVEN
        // An admin user already exists
        UsuarioData admin1 = new UsuarioData();
        admin1.setEmail("admin1@umh.es");
        admin1.setPassword("1234");
        admin1.setNombre("Admin 1");
        admin1.setRol(todolist.model.UsuarioRol.ADMIN);
        usuarioService.registrar(admin1);

        // WHEN, THEN
        // Trying to register another admin throws an exception
        UsuarioData admin2 = new UsuarioData();
        admin2.setEmail("admin2@umh.es");
        admin2.setPassword("1234");
        admin2.setNombre("Admin 2");
        admin2.setRol(todolist.model.UsuarioRol.ADMIN);

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(admin2);
        });
    }


}