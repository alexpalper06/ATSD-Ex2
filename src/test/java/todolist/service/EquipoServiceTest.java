package todolist.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import todolist.dto.EquipoData;
import todolist.dto.EquipoDetalleData;
import todolist.dto.UsuarioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import todolist.model.Equipo;

import java.util.List;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Test
    public void crearRecuperarEquipo() {

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }

    @Test
    public void listadoEquiposOrdenAlfabetico() {
        // GIVEN
        // Dos equipos en la base de datos
        equipoService.crearEquipo("Proyecto BBB");
        equipoService.crearEquipo("Proyecto AAA");

        // WHEN
        // Recuperamos los equipos
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        // THEN
        // Los equipos están ordenados por nombre
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto AAA");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto BBB");
    }

    @Test
    public void añadirUsuarioAEquipoTest() {
        // GIVEN
        // Un usuario y un equipo en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        // Añadimos el usuario al equipo
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // THEN
        // El usuario pertenece al equipo
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@umh");
    }

    @Test
    public void recuperarEquiposDeUsuario() {
        // GIVEN
        // Un usuario y dos equipos en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo1 = equipoService.crearEquipo("Project 1");
        EquipoData equipo2 = equipoService.crearEquipo("Project 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), usuario.getId());

        // WHEN
        // Recuperamos los equipos del usuario
        List<EquipoData> equipos = equipoService.equiposUsuario(usuario.getId());

        // THEN
        // El usuario pertenece a los dos equipos
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Project 1");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Project 2");
    }
    @Test
    public void comprobarExcepciones() {
        // Comprobamos las excepciones lanzadas por los métodos
        // recuperarEquipo, añadirUsuarioAEquipo, usuariosEquipo y equiposUsuario
        assertThatThrownBy(() -> equipoService.recuperarEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(1L, 1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.usuariosEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.equiposUsuario(1L))
                .isInstanceOf(EquipoServiceException.class);

        // Creamos un equipo pero no un usuario
        // y comprobamos que también se lanza una excepción
        EquipoData equipo = equipoService.crearEquipo("Project 1");
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class);

        // Comprobar que crear equipo con nombre duplicado lanza excepcion
        assertThatThrownBy(() -> equipoService.crearEquipo("Project 1"))
                .isInstanceOf(EquipoServiceException.class);
    }


    @Test
    public void eliminarUsuarioDeEquipoTest() {
        // Given
        // Users added to a team
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh.es");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("user2@umh.es");
        usuario2.setPassword("1234");
        usuario2 = usuarioService.registrar(usuario2);

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario2.getId());
        // When
        // The user is deleted from the team
        equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario.getId());

        // Then
        // The team has no members already
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios.size()).isEqualTo(1);
    }

    @Test
    public void eliminarUltimoUsuarioEliminaEquipoTest() {
        // Given
        // User added to a team
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh.es");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // When
        // The user is deleted from the team
        equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario.getId());

        // The team is deleted from the database
        assertThatThrownBy(()-> equipoService.recuperarEquipo(equipo.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessageContaining("El equipo no existe");
    }

    @Test
    public void eliminarUsuariDeEquipoActualizaRelacionTest() {
        // Given
        UsuarioData usuario1 = new UsuarioData();
        usuario1.setEmail("user1@umh.es");
        usuario1.setPassword("1234");
        usuario1 = usuarioService.registrar(usuario1);

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("user2@umh.es");
        usuario2.setPassword("1234");
        usuario2 = usuarioService.registrar(usuario2);

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario1.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario2.getId());

        // When
        equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario1.getId());

        // Then
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user2@umh.es");
    }

    @Test
    public void crearEquipoAnyadeUsuarioTest() {
        // When a new team is created by a user,
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh.es");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);

        // Simulates the process where a user creates their team in the web
        EquipoData equipo = equipoService.crearEquipoConUsuario("Proyecto 1", usuario.getId());

        // THEN
        // Team is created
        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");

        // The user pertains to the team
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@umh.es");
    }

    @Test
    public void compruebaUsuarioEstaEnEquipoTest() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh.es");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);


        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("user2@umh.es");
        usuario2.setPassword("1234");
        usuario2 = usuarioService.registrar(usuario2);

        // Simulates the process where a user creates their team in the web
        EquipoData equipo = equipoService.crearEquipoConUsuario("Proyecto 1", usuario.getId());

        // THEN
        // Team is created
        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");

        // Checks whether the user is in the team or not
        assertThat(equipoService.esUsuarioMiembro(usuario.getId(), equipoBd.getId())).isTrue();
        assertThat(equipoService.esUsuarioMiembro(usuario2.getId(), equipoBd.getId())).isFalse();

    }

     @Test
    public void recuperarDetallesEquipoTest() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh.es");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);


        // Simulates the process where a user creates their team in the web
        EquipoData equipo = equipoService.crearEquipoConUsuario("Proyecto 1", usuario.getId());
        EquipoDetalleData equipoDetalle = equipoService.recuperarEquipoDetalle(equipo.getId());
        assertThat(equipoDetalle).isNotNull();
        assertThat(equipoDetalle.getNombre()).isEqualTo("Proyecto 1");
        assertThat(equipoDetalle.getUsuarios()).hasSize(1);
    }
}
