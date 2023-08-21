package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;


import java.util.ArrayList;

@Transactional
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

 @Query(value = "SELECT * FROM search_engine.lemma WHERE lemma = ?1 and site_id = ?2",
            nativeQuery = true)
   Lemma findByLemmaAndSite_id(String lemma, int site_id);
    @Query(value = "SELECT * FROM search_engine.lemma WHERE lemma = ?1",
            nativeQuery = true)
    ArrayList<Lemma> findAllByLemma(String lemma);

    @Query(value = "SELECT id FROM search_engine.lemma WHERE lemma = ?1 and site_id = ?2",
            nativeQuery = true)
    int findIdByLemmaAndSite_id(String lemma, int site_id);

    @Query(value = "SELECT id FROM search_engine.lemma WHERE lemma = ?1",
            nativeQuery = true)
    ArrayList<Integer> findAllIdByLemma(String lemma);

    @Modifying
    @Transactional
    @Query(value = "UPDATE search_engine.lemma l SET l.frequency = (frequency + 1) WHERE l.id = ?1",
            nativeQuery = true)
    void updateContentLemma(int id);

 @Query(value = "SELECT id FROM search_engine.lemma WHERE lemma = ?1 and site_id = ?2",
         nativeQuery = true)
 ArrayList<Integer> findAllIdByLemmaAndSite_id(String lemma, int siteId);
}
