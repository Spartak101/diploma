package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexObject;

@Repository
public interface IndexObjectRepository extends JpaRepository<IndexObject, Integer> {
}
