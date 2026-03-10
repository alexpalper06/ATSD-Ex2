package todolist.service;

import todolist.model.Tarea;
import todolist.model.Usuario;
import todolist.repository.TareaRepository;
import todolist.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
// Se ejecuta solo si el perfil activo es 'dev'
@Profile("dev")
public class InitDbService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TareaRepository tareaRepository;

    // Se ejecuta tras crear el contexto de la aplicación
    // para inicializar la base de datos
    @PostConstruct
    public void initDatabase() {
        /*
        This should never be done. The information must never be hardcoded, it should instead be stored on a safe
        file, and should never be uploaded into the repository.
         */
        Usuario usuario = new Usuario("richard@umh.es");
        usuario.setNombre("Richard Stallman");
        usuario.setPassword("1234");
        usuarioRepository.save(usuario);

        Tarea tarea1 = new Tarea(usuario, "Create the GNU General Public License");
        tareaRepository.save(tarea1);

        Tarea tarea2 = new Tarea(usuario, "Buy milk, cereals and coffee");
        tareaRepository.save(tarea2);

        Usuario userMe = new Usuario("alex.palacios@goumh.umh.es");
        userMe.setNombre("Alex Palacios Perez");

        userMe.setPassword("1234abc");
        usuarioRepository.save(userMe);

        Tarea taskMe = new Tarea(userMe, "Create my own username");
        tareaRepository.save(taskMe);

    }

}
