package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import searchengine.model.StopObject;

@Repository
public interface StopObjectRepository  extends JpaRepository<StopObject, Integer> {
}
