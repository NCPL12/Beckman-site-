package ncpl.bms.reports.model.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "report_template")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "VARBINARY(MAX)")
    private byte[] parameters;

    private String additionalInfo;

    private String report_group;

    // ✅ New fields
    private String roomId;

    private String roomName;

    // Setter for List<String> -> byte[]
    public void setParameters(List<String> parameters) {
        this.parameters = serializeList(parameters);
    }

    // Getter for byte[] -> List<String>
    public List<String> getParameters() {
        return deserializeList(this.parameters);
    }

    private byte[] serializeList(List<String> list) {
        if (list == null) return null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(list);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing list", e);
        }
    }

    private List<String> deserializeList(byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (List<String>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error deserializing list", e);
        }
    }
}
