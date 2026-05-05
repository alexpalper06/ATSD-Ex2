package todolist.service;

import todolist.dto.EquipoData;
import todolist.model.Equipo;
import todolist.repository.EquipoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private ModelMapper modelMapper;


    @Transactional
    public EquipoData crearEquipo(String nombre) {
        Optional<Equipo> equipoBD = equipoRepository.findByNombre(nombre);
        if (equipoBD.isPresent())
            throw new EquipoServiceException("El equipo " + nombre + " ya está registrado");
        else if (nombre == null)
            throw new EquipoServiceException("El equipo no tiene nombre");
        else {
            Equipo equipoNuevo = modelMapper.map(new Equipo(), Equipo.class);
            equipoNuevo.setNombre(nombre);
            equipoNuevo = equipoRepository.save(equipoNuevo);

            return modelMapper.map(equipoNuevo, EquipoData.class);
        }
    }

    @Transactional
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        
        return modelMapper.map(equipo, EquipoData.class);
    }
}
