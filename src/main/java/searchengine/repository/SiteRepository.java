package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

@Transactional
@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

   @Query(value = "SELECT id FROM search_engine.site WHERE url = ?1",
           nativeQuery = true)
   int findIdByUrl(String url);

   Site findById(int idSite);

   Site findByUrl(String path);
}
