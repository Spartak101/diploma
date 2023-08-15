package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "SELECT id FROM search_engine.page WHERE path = ?1",
            nativeQuery = true)
    int findIdByPath(String pathHtml);
}
