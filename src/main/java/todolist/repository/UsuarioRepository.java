package todolist.repository;

import todolist.model.Usuario;
import todolist.model.UsuarioRol;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

/* We want to allow pagination on users for the list,
*  so we extend the Spring pagination repository
* https://www.baeldung.com/spring-data-jpa-pagination-sorting
 */
public interface UsuarioRepository extends PagingAndSortingRepository<Usuario, Long>, CrudRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String s);
    boolean existsByRol(UsuarioRol rol);
}
