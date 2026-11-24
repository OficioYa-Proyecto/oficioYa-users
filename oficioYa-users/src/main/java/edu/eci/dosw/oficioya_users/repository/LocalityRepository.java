package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.LocalityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalityRepository extends JpaRepository<LocalityEntity, Long> {
    List<LocalityEntity> findAllByOrderByNameAsc();
}