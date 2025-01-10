package com.quolance.quolance_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quolance.quolance_api.dtos.LoginRequestDto;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.helpers.EntityCreationHelper;
import com.quolance.quolance_api.repositories.ProjectRepository;
import com.quolance.quolance_api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class AdminControllerIntegrationTest extends AbstractTestcontainers {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    private User client;

    private MockHttpSession session;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(EntityCreationHelper.createAdmin());
        client = userRepository.save(EntityCreationHelper.createClient());
    }

    @BeforeEach
    void setUpSession() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword("Password123!");

        session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    void getAllPendingProjectIsOk() throws Exception {
        // Arrange
        Project pendingProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.PENDING, client));
        Project openProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.OPEN, client));

        // Act
        String response = mockMvc.perform(get("/api/admin/projects/pending/all")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("direction", "desc")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageResponse = objectMapper.readValue(response, Map.class);
        List<Object> content = (List<Object>) pageResponse.get("content");

        // Assert
        assertThat(content.size()).isEqualTo(1);
        assertThat(pageResponse.get("totalElements")).isEqualTo(1);
        assertThat(pageResponse.get("totalPages")).isEqualTo(1);
        assertThat(pageResponse.get("number")).isEqualTo(0);
        assertThat(pageResponse.get("size")).isEqualTo(10);

        Map<String, Object> projectResponse = (Map<String, Object>) content.get(0);
        assertThat(projectResponse.get("id")).isEqualTo(pendingProject.getId().intValue());
        assertThat(projectResponse.get("projectStatus")).isEqualTo("PENDING");
    }

    @Test
    void approvePendingProjectIsOkBecomesApproved() throws Exception {
        // Arrange
        Project pendingProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.PENDING, client));

        // Act
        String response = mockMvc.perform(post("/api/admin/projects/pending/" + pendingProject.getId() + "/approve")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(response).isEqualTo("Project approved successfully");

        Project approvedProject = projectRepository.findById(pendingProject.getId()).get();
        assertThat(approvedProject.getProjectStatus()).isEqualTo(ProjectStatus.OPEN);
    }

    @Test
    void approveOpenProjectIsConflict() throws Exception {
        // Arrange
        Project openProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.OPEN, client));

        // Act
        String response = mockMvc.perform(post("/api/admin/projects/pending/" + openProject.getId() + "/approve")
                        .session(session))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);
        assertThat(jsonResponse.get("message")).isEqualTo("Project is already in status: OPEN");
    }

    @Test
    void approveClosedProjectIsForbidden() throws Exception {
        // Arrange
        Project closedProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.CLOSED, client));

        // Act
        String response = mockMvc.perform(post("/api/admin/projects/pending/" + closedProject.getId() + "/approve")
                        .session(session))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);
        assertThat(jsonResponse.get("message")).isEqualTo("Project is closed and cannot be updated.");
        assertThat(projectRepository.findById(closedProject.getId()).get().getProjectStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    void rejectPendingProjectIsOkBecomesRejected() throws Exception {
        // Arrange
        Project pendingProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.PENDING, client));

        // Act
        String response = mockMvc.perform(post("/api/admin/projects/pending/" + pendingProject.getId() + "/reject")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(response).isEqualTo("Project rejected successfully");

        Project rejectedProject = projectRepository.findById(pendingProject.getId()).get();
        assertThat(rejectedProject.getProjectStatus()).isEqualTo(ProjectStatus.REJECTED);
    }


    @Test
    void rejectClosedProjectIsForbidden() throws Exception {
        // Arrange
        Project closedProject = projectRepository.save(EntityCreationHelper.createProject(ProjectStatus.CLOSED, client));

        // Act
        String response = mockMvc.perform(post("/api/admin/projects/pending/" + closedProject.getId() + "/reject")
                        .session(session))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);
        assertThat(jsonResponse.get("message")).isEqualTo("Project is closed and cannot be updated.");
        assertThat(projectRepository.findById(closedProject.getId()).get().getProjectStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

}
