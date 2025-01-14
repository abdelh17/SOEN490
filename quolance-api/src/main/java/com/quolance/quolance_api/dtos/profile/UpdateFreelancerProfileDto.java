package com.quolance.quolance_api.dtos.profile;

import com.quolance.quolance_api.entities.enums.Availability;
import com.quolance.quolance_api.entities.enums.FreelancerExperienceLevel;
import com.quolance.quolance_api.entities.enums.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFreelancerProfileDto {
    private String firstName;
    private String lastName;
    private String bio;
    private String contactEmail;
    private String city;
    private String state;
    private FreelancerExperienceLevel experienceLevel;
    private Set<String> socialMediaLinks;
    private Set<Tag> skills;
    private Availability availability;
}