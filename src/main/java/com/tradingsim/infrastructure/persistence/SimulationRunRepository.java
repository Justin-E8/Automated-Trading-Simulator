package com.tradingsim.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long> {
}
