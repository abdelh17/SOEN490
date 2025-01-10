package com.quolance.quolance_api.services.business_workflow.impl;

import com.quolance.quolance_api.dtos.UpdateUserRequestDto;
import com.quolance.quolance_api.dtos.UserResponseDto;
import com.quolance.quolance_api.dtos.application.ApplicationCreateDto;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.profile.FreelancerProfileDto;
import com.quolance.quolance_api.dtos.profile.UpdateFreelancerProfileDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.Profile;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.business_workflow.FreelancerWorkflowService;
import com.quolance.quolance_api.services.entity_services.ApplicationService;
import com.quolance.quolance_api.services.entity_services.FileService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.services.entity_services.UserService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FreelancerWorkflowServiceImpl implements FreelancerWorkflowService {

    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final UserService userService;
    private final FileService fileService;

    @Override
    public void submitApplication(ApplicationCreateDto applicationCreateDto, User freelancer) {

        if(applicationService.getApplicationByFreelancerIdAndProjectId(freelancer.getId(), applicationCreateDto.getProjectId()) != null){
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("You have already applied to this project.")
                    .build();
        }

        try {
            Application application = ApplicationCreateDto.toEntity(applicationCreateDto);
            Project project = projectService.getProjectById(applicationCreateDto.getProjectId());

            if(!project.isProjectApproved()){
                throw ApiException.builder()
                        .status(HttpServletResponse.SC_CONFLICT)
                        .message("You can't apply to this project.")
                        .build();
            }

            application.setFreelancer(freelancer);
            application.setProject(project);

            applicationService.saveApplication(application);

        } catch (OptimisticLockException e) {
            handleOptimisticLockException(e);
        }

    }

    // Might remove this method
    @Override
    public ApplicationDto getApplication(Long applicationId, User freelancer) {
        Application application = applicationService.getApplicationById(applicationId);
        if (!application.isOwnedBy(freelancer.getId())) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("You are not authorized to view this application.")
                    .build();
        }
        return ApplicationDto.fromEntity(application);
    }

    @Override
    public void deleteApplication(Long applicationId, User freelancer) {
        Application application = applicationService.getApplicationById(applicationId);
        if (!application.isOwnedBy(freelancer.getId())) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("You are not authorized to delete this application.")
                    .build();
        }
        applicationService.deleteApplication(application);
    }

    @Override
    public Page<ApplicationDto> getAllFreelancerApplications(User freelancer, Pageable pageable) {
        Page<Application> applicationPage = applicationService.getAllApplicationsByFreelancerId(freelancer.getId(), pageable);
        return applicationPage.map(ApplicationDto::fromEntity);
    }

    //Alternatively, properly paginate this endpoint
    @Override
    public Page<ProjectPublicDto> getAllAvailableProjects(Pageable pageable) {
        LocalDate currentDate = LocalDate.now();

        Page<Project> openAndClosedProjects = projectService.getProjectsByStatuses(
                List.of(ProjectStatus.OPEN, ProjectStatus.CLOSED),
                pageable
        );

        List<ProjectPublicDto> filteredProjects = openAndClosedProjects.getContent().stream()
                .filter(project -> !(
                        project.getProjectStatus().equals(ProjectStatus.CLOSED) &&
                                project.getVisibilityExpirationDate().isBefore(currentDate)
                ))
                .map(ProjectPublicDto::fromEntity)
                .toList();

        return new PageImpl<>(
                filteredProjects,
                pageable,
                openAndClosedProjects.getTotalElements()
        );
    }

    @Override
    public ProjectPublicDto getProject(Long projectId) {

        Project project = projectService.getProjectById(projectId);
        LocalDate currentDate = LocalDate.now();

        if(project.getProjectStatus().equals(ProjectStatus.PENDING)){
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("Project is not yet approved.")
                    .build();
        }
        if (project.getVisibilityExpirationDate() != null &&
                project.getVisibilityExpirationDate().isBefore(currentDate)) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("Project visibility has expired.")
                    .build();
        }
        return ProjectPublicDto.fromEntity(project);
    }

    @Override
    public FreelancerProfileDto getFreelancerProfile(String username) {
        Optional<User> freelancer = userService.findByUsername(username);

        if (freelancer.isPresent()) {
            return FreelancerProfileDto.fromEntity(freelancer.get());
        } else {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_NOT_FOUND)
                    .message("Freelancer not found")
                    .build();
        }
    }

    @Override
    public void updateFreelancerProfile(UpdateFreelancerProfileDto updateFreelancerProfileDto, User freelancer) {
        try {
            Profile profile = freelancer.getProfile();
            if (profile == null) {
                profile = new Profile();
                profile.setUser(freelancer);
                freelancer.setProfile(profile);
            }

            // Update all profile fields with new values from DTO
            profile.setBio(updateFreelancerProfileDto.getBio());
            profile.setContactEmail(updateFreelancerProfileDto.getContactEmail());
            profile.setCity(updateFreelancerProfileDto.getCity());
            profile.setState(updateFreelancerProfileDto.getState());
            profile.setExperienceLevel(updateFreelancerProfileDto.getExperienceLevel());
            profile.setSocialMediaLinks(updateFreelancerProfileDto.getSocialMediaLinks());
            profile.setSkills(updateFreelancerProfileDto.getSkills());
            profile.setAvailability(updateFreelancerProfileDto.getAvailability());

            UpdateUserRequestDto updateUserRequestDto = UpdateUserRequestDto.builder()
                    .firstName(updateFreelancerProfileDto.getFirstName())
                    .lastName(updateFreelancerProfileDto.getLastName())
                    .build();

            UserResponseDto UserResponseDto = userService.updateUser(updateUserRequestDto, freelancer);
            System.out.printf("UserResponseDto: %s", UserResponseDto);

        } catch (OptimisticLockException e) {
            handleOptimisticLockException(e);
        } catch (Exception e) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .message("Failed to update profile")
                    .errors(Map.of("updateError", e.getMessage()))
                    .build();
        }
    }

    @Override
    public void uploadProfilePicture(MultipartFile photo, User freelancer) {
        try{
            Map<String, Object> uploadResult = fileService.uploadFile(photo, freelancer);
            String photoUrl = uploadResult.get("secure_url").toString();

            userService.updateProfilePicture(freelancer, photoUrl);


        } catch (Exception e) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .message("Failed to upload profile picture")
                    .errors(Map.of("uploadError", e.getMessage()))
                    .build();
        }

    }

    // Helper method to handle optimistic lock exception
    private void handleOptimisticLockException(Exception e) {
        throw ApiException.builder()
                .status(HttpServletResponse.SC_CONFLICT)
                .message("The ressource was modified by another user. Please refresh the page and try again.")
                .errors(Map.of("optimisticLock", e.getMessage()))
                .build();
    }

}