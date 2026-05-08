package todolist.dto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EquipoDetalleData {

    private Long id;
    private String nombre;

    private Set<UsuarioData> usuarios = new HashSet<>();

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Set<UsuarioData> getUsuarios() { return usuarios; }
    public void setUsuarios(Set<UsuarioData> usuarios) { this.usuarios = usuarios; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquipoDetalleData that = (EquipoDetalleData) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }

        return nombre.equals(that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}
