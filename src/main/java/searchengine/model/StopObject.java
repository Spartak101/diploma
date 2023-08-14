package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StopObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String pathHtml;

}
