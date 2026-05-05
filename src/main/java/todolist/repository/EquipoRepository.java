package todolist.repository;

import org.springframework.data.repository.CrudRepository;
import todolist.model.Equipo;
import todolist.model.Usuario;

import java.util.Optional;

public interface EquipoRepository extends CrudRepository<Equipo, Long>{
    Optional<Equipo> findByNombre(String s);
}
