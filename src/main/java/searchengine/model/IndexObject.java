package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;



@Entity
@Data
public class IndexObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id", columnDefinition = "INT", nullable = false)
    private Page page;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", columnDefinition = "INT", nullable = false)
    private Lemma lemma;
    @Column(columnDefinition = "FLOAT", nullable = false)
    private Float runk;
}
