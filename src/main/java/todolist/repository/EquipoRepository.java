package todolist.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import todolist.model.Equipo;
import todolist.model.Usuario;

import java.awt.print.Pageable;
import java.util.List;

import java.util.Optional;

public interface EquipoRepository extends PagingAndSortingRepository<Equipo, Long> ,CrudRepository<Equipo, Long>{
    Optional<Equipo> findByNombre(String s);

    public List<Equipo> findAll();

    public Page<Equipo> findAll(Pageable pageable);
}
