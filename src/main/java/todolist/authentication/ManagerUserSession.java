package todolist.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import todolist.model.UsuarioRol;

import javax.servlet.http.HttpSession;

@Component
public class ManagerUserSession {

    @Autowired
    HttpSession session;

    // Añadimos el id de usuario en la sesión HTTP para hacer
    // una autorización sencilla. En los métodos de controllers
    // comprobamos si el id del usuario logeado coincide con el obtenido
    // desde la URL
    public void logearUsuario(Long idUsuario, String username, UsuarioRol rol) {

        session.setAttribute("idUsuarioLogeado", idUsuario);
        session.setAttribute("username", username);
        session.setAttribute("rolUsuarioLogeado", rol);
    }

    public Long usuarioLogeado() {
        return (Long) session.getAttribute("idUsuarioLogeado");
    }

    public String usernameUsuarioLogeado() {
        return (String) session.getAttribute("username");
    }

    public UsuarioRol rolUsuarioLogeado() {
        return (UsuarioRol) session.getAttribute("rolUsuarioLogeado");
    }

    public boolean isAdmin() {
        UsuarioRol rol = (UsuarioRol) session.getAttribute("rolUsuarioLogeado");
        return rol != null && rol == UsuarioRol.ADMIN;
    }

    public void logout() {
        session.setAttribute("idUsuarioLogeado", null);
    }
}
