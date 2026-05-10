package com.tradingsim.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted simulation runs.
 */
public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long> {

    Page<SimulationRunEntity> findAllBySymbolContainingIgnoreCaseAndStrategyNameContainingIgnoreCase(
            String symbolFilter,
            String strategyFilter,
            Pageable pageable
    );
}
