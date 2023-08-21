package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.StopObject;

import java.util.ArrayList;

@Transactional
@Repository
public interface StopObjectRepository  extends JpaRepository<StopObject, Integer> {


    @Query(value = "SELECT path_html FROM search_engine.stop_object WHERE site_id = ?1",
            nativeQuery = true)
    ArrayList<String> findAllBySite_Id(int site_id);
    @Query(value = "SELECT * FROM search_engine.stop_object WHERE site_id = ?1",
            nativeQuery = true)
    ArrayList<StopObject> findAllBySite_id(int site_id);
}
