package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.ArrayList;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "SELECT id FROM search_engine.page WHERE path = ?1",
            nativeQuery = true)
    int findIdByPath(String pathHtml);
    @Query(value = "SELECT path FROM search_engine.page WHERE site_id = ?1",
            nativeQuery = true)
    ArrayList<String> findPathBySite_id(int site_id);
}
