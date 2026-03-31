package com.msp.backend.modules.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long>,
        JpaSpecificationExecutor<Settlement> {

    List<Settlement> findAllByOrderBySettlementDateDesc();

    List<Settlement> findByCreditAdviceId(Long creditAdviceId);

    List<Settlement> findByCreditAdviceIdInOrderBySettlementDateDesc(List<Long> creditAdviceIds);

    List<Settlement> findBySettlementNoContainingIgnoreCase(String settlementNo);

    Page<Settlement> findByCreditAdviceIdIn(List<Long> creditAdviceIds, Pageable pageable);
}
