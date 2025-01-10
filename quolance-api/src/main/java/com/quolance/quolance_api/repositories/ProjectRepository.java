package com.quolance.quolance_api.repositories;

import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findProjectsByClientId(Long clientId, Pageable pageable);

    Page<Project> findProjectsByProjectStatusIn(List<ProjectStatus> projectStatuses, Pageable pageable);
}
