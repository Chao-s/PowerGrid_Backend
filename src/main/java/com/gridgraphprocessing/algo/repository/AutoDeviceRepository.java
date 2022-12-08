package com.gridgraphprocessing.algo.repository;

import com.gridgraphprocessing.algo.model.AutoDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AutoDeviceRepository extends JpaRepository<AutoDevice, String> {
    boolean existsByIsAutoAndId(Integer isAuto, String id);

    @Query(value = "select a.is_auto from decloud_autodevice_account a where a.id=?1", nativeQuery = true)
    Integer findIsAutoById(String id);

}
