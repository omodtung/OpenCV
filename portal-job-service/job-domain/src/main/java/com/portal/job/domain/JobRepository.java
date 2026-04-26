package com.portal.job.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {
  List<Job> findByEmployerId(UUID employerId);

  List<Job> findByStatus(String status);

  @Modifying
  @Query(
    "UPDATE Job j SET j.companyName = :newName WHERE j.employerId = :companyId"
  )
  int updateCompanyName(
    @Param("companyId") UUID companyId,
    @Param("newName") String newName
  );
}
