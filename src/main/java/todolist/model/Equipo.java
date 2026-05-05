package todolist.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "equipos")
    public class Equipo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @NotNull
        private String nombre;

        // Constructor vacío necesario para JPA/Hibernate.
        // No debe usarse desde la aplicación.
        public Equipo() {}

        // Al crear una tarea la asociamos automáticamente a un usuario
        public Equipo(String nombre) {
            this.nombre = nombre;
        }

        // Getters y setters básicos

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String titulo) {
            this.nombre = titulo;
        }

        // equals para el tercer test
        // que aparece en la prácitca

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            todolist.model.Equipo equipo = (todolist.model.Equipo) o;
            if (id != null && equipo.id != null)
                // Si tenemos los ID, comparamos por ID
                return Objects.equals(id, equipo.id);
            // si no comparamos por campos obligatorios
            return nombre.equals(equipo.nombre);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nombre);
        }
}
