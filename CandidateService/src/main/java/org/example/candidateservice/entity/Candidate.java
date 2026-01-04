package org.example.candidateservice.entity;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String headline;
    private Integer yearsOfExperience;
    private String currentCompany;
    // Assuming userId will be linked to Identity Service, for now, a simple String
    private UUID userId; // Using UUID as mentioned in DesignFlow.txt

    public Candidate() {
    }

    public Candidate(String firstName, String lastName, String email, String phoneNumber, String headline, Integer yearsOfExperience, String currentCompany, UUID userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.headline = headline;
        this.yearsOfExperience = yearsOfExperience;
        this.currentCompany = currentCompany;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Candidate{"
               + "id=" + id + ", "
               + "firstName='" + firstName + "'" + ", "
               + "lastName='" + lastName + "'" + ", "
               + "email='" + email + "'" + ", "
               + "phoneNumber='" + phoneNumber + "'" + ", "
               + "headline='" + headline + "'" + ", "
               + "yearsOfExperience=" + yearsOfExperience + ", "
               + "currentCompany='" + currentCompany + "'" + ", "
               + "userId='" + userId + "'" + 
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(id, candidate.id) &&
               Objects.equals(firstName, candidate.firstName) &&
               Objects.equals(lastName, candidate.lastName) &&
               Objects.equals(email, candidate.email) &&
               Objects.equals(phoneNumber, candidate.phoneNumber) &&
               Objects.equals(headline, candidate.headline) &&
               Objects.equals(yearsOfExperience, candidate.yearsOfExperience) &&
               Objects.equals(currentCompany, candidate.currentCompany) &&
               Objects.equals(userId, candidate.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, phoneNumber, headline, yearsOfExperience, currentCompany, userId);
    }
}
