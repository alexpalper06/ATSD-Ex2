package todolist.dto;

import java.util.Objects;

// Data Transfer Object for displaying a preview of users in the list
public class UserPreviewData {

    private Long id;
    private String nombre; // Variable name in spanish since Usuario model data has name in spanish
    private String email;

    // Default constructor
    public UserPreviewData() {
    }

    // Constructor with parameters
    public UserPreviewData(Long id, String name, String email) {
        this.id = id;
        this.nombre = name;
        this.email = email;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPreviewData)) return false;
        UserPreviewData that = (UserPreviewData) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
