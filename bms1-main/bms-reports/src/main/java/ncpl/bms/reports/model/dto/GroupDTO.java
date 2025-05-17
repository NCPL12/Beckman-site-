package ncpl.bms.reports.model.dto;

public class GroupDTO {

    private Long id;
    private String name;

    // No-argument constructor
    public GroupDTO() {}

    // Parameterized constructor
    public GroupDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getter and Setter for id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter and Setter for name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

