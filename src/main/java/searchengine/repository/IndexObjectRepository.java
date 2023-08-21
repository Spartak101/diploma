package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Repository
public interface IndexObjectRepository extends JpaRepository<IndexObject, Integer> {


    default ArrayList<Integer> findAllByLemma_idIn(ArrayList<Integer> lemmaId){
        Set<Integer> pages_id = new HashSet<>();
        for (int id : lemmaId) {
            int page_id = findPage_idByLemma_id(id);
            pages_id.add(page_id);
        }
        return (ArrayList<Integer>) pages_id.stream().collect(Collectors.toList());
    }
    @Query(value = "SELECT page_id FROM search_engine.index_object WHERE lemma_id = ?1",
            nativeQuery = true)
    int findPage_idByLemma_id(int id);
}
