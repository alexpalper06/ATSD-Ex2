package todolist.service;

import todolist.dto.EquipoData;
import todolist.dto.EquipoDetalleData;
import todolist.dto.UsuarioData;
import todolist.model.Equipo;
import todolist.model.Usuario;
import todolist.repository.EquipoRepository;
import todolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Se añade un equipo en la aplicación.
    // El nombre debe ser distinto de null
    // El nombre no debe estar registrado en la base de datos
    @Transactional
    public EquipoData registrar(EquipoData equipo) {
        Optional<Equipo> equipoBD = equipoRepository.findByNombre(equipo.getNombre());
        if (equipoBD.isPresent())
            throw new EquipoServiceException("El equipo " + equipo.getNombre() + " ya está registrado");
        else if (equipo.getNombre() == null)
            throw new EquipoServiceException("El equipo no tiene nombre");
        else {
            Equipo equipoNuevo = modelMapper.map(equipo, Equipo.class);
            equipoNuevo = equipoRepository.save(equipoNuevo);
            return modelMapper.map(equipoNuevo, EquipoData.class);
        }
    }

    @Transactional(readOnly = true)
    public EquipoData findByNombre(String nombre) {
        Equipo equipo = equipoRepository.findByNombre(nombre).orElse(null);
        if (equipo == null) return null;
        else {
            return modelMapper.map(equipo, EquipoData.class);
        }
    }

    @Transactional(readOnly = true)
    public EquipoData findById(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        if (equipo == null) return null;
        else {
            return modelMapper.map(equipo, EquipoData.class);
        }
    }

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
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public EquipoDetalleData recuperarEquipoDetalle(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");
        
        // Map directly to DetalleData
        EquipoDetalleData equipoDTO = modelMapper.map(equipo, EquipoDetalleData.class);
        
        // Convert the List of users to a Set for the DTO
        Set<UsuarioData> usuarios = equipo.getUsuarios().stream()
                .map(u -> modelMapper.map(u, UsuarioData.class))
                .collect(Collectors.toSet());
                
        equipoDTO.setUsuarios(usuarios);
        return equipoDTO;
    }

    @Transactional
    public List<EquipoData> findAllOrdenadoPorNombre() {
        // recuperamos todos los equipos
        List<Equipo> equipos;
        equipos = equipoRepository.findAll();

        // cambiamos el tipo de la lista de equipos
        List<EquipoData> equiposData = equipos.stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());

        // ordenamos la lista por nombre del equipo
        Collections.sort(equiposData, (a, b) -> a.getNombre().compareTo(b.getNombre()));
        return equiposData;
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long idEquipo, Long idUsuario) {
        // recuperamos el equipo
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null) throw new EquipoServiceException("El equipo no existe");

        // recuperamos el usuario
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) throw new EquipoServiceException("El usuario no existe");

        // comprobamos que el usuario no pertenece al equipo
        if (equipo.getUsuarios().contains(usuario))
            throw new EquipoServiceException("El usuario ya pertenece al equipo");

        // añadimos el usuario al equipo
        equipo.addUsuario(usuario);
        // guardamos el equipo
        equipoRepository.save(equipo);
        // guardamos el usuario
        usuarioRepository.save(usuario);
        // con ello se guarda la relación
    }

    @Transactional
    public List<UsuarioData> usuariosEquipo(Long idEquipo) {
        // recuperamos el equipo
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");

        // cambiamos el tipo de la lista de usuarios
        List<UsuarioData> usuarios = equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
        return usuarios;
    }

    @Transactional
    public List<EquipoData> equiposUsuario(long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null)
            throw new EquipoServiceException("El usuario no existe");

        // cambiamos el tipo de la lista de equipos
        List<EquipoData> equipos = usuario.getEquipos().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
        return equipos;

    }

    @Transactional(readOnly = true)
    public Page<EquipoData> findAllTeamsPreview(Pageable pageable) {
        Page<Equipo> equiposPage = equipoRepository.findAll(pageable);

        return equiposPage.map(equipo -> modelMapper.map(equipo, EquipoData.class));
    }

    // Methods for user story 009
    @Transactional
    public void eliminarUsuarioDeEquipo(Long idEquipo, Long idUsuario) {
        // Recover the team
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);

        if (equipo == null) {
            throw new EquipoServiceException("El equipo no existe");
        }

        // Recover the user
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("El usuario no existe");
        }

        // Delete the user from the team updating both sides
        equipo.removeUsuario(usuario);

        if (equipo.getUsuarios().isEmpty()) {
            borrarEquipo(equipo.getId());
        }
    }

    @Transactional
    public void borrarEquipo(Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo)
                .orElseThrow(() -> new EquipoServiceException("El equipo no existe"));

        equipoRepository.delete(equipo);
    }

    @Transactional
    public EquipoData crearEquipoConUsuario(String nombre, Long idUsuario) {

        // Crear equipo
        EquipoData nuevoEquipo = crearEquipo(nombre);
        // Añadir automaticamente al usuario
        // Usamos el ID que nos devolvió el metodo anterior y el de la sesión
        añadirUsuarioAEquipo(nuevoEquipo.getId(), idUsuario);

        // 4. Devolvemos el equipo actualizado (con el usuario ya dentro)
        return nuevoEquipo;
    }

    @Transactional
    public boolean esUsuarioMiembro(Long usuarioId, Long equipoId) {
        return equipoRepository.existsByUsuariosIdAndId(usuarioId, equipoId);
    }

    
}

