package com.quolance.quolance_api.repositories;

import com.quolance.quolance_api.entities.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findApplicationsByFreelancerId(Long freelancerId, Pageable pageable);
    Page<Application> findApplicationsByProjectId(Long projectId, Pageable pageable);
    Application findApplicationByFreelancerIdAndProjectId(Long freelancerId, Long projectId);
}
