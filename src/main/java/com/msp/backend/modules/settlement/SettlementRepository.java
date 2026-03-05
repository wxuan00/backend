package com.msp.backend.modules.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByOrderBySettlementDateDesc();

    List<Settlement> findByCreditAdviceId(Long creditAdviceId);

    List<Settlement> findByCreditAdviceIdInOrderBySettlementDateDesc(List<Long> creditAdviceIds);

    List<Settlement> findBySettlementNoContainingIgnoreCase(String settlementNo);
}
