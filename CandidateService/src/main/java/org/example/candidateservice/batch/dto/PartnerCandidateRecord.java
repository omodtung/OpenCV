package org.example.candidateservice.batch.dto;

import java.util.Objects;

public class PartnerCandidateRecord {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String primarySkill;
    private String yearsOfExperience; // Read as String, will be converted to Integer
    private String currentCompany;

    // Default constructor for Spring Batch
    public PartnerCandidateRecord() {
    }

    public PartnerCandidateRecord(String email, String firstName, String lastName, String phone, String primarySkill, String yearsOfExperience, String currentCompany) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.primarySkill = primarySkill;
        this.yearsOfExperience = yearsOfExperience;
        this.currentCompany = currentCompany;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPrimarySkill() {
        return primarySkill;
    }

    public void setPrimarySkill(String primarySkill) {
        this.primarySkill = primarySkill;
    }

    public String getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(String yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    @Override
    public String toString() {
        return "PartnerCandidateRecord{" +
               "email='" + email + "'" +
               ", firstName='" + firstName + "'" +
               ", lastName='" + lastName + "'" +
               ", phone='" + phone + "'" +
               ", primarySkill='" + primarySkill + "'" +
               ", yearsOfExperience='" + yearsOfExperience + "'" +
               ", currentCompany='" + currentCompany + "'" +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerCandidateRecord that = (PartnerCandidateRecord) o;
        return Objects.equals(email, that.email) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(phone, that.phone) &&
               Objects.equals(primarySkill, that.primarySkill) &&
               Objects.equals(yearsOfExperience, that.yearsOfExperience) &&
               Objects.equals(currentCompany, that.currentCompany);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, firstName, lastName, phone, primarySkill, yearsOfExperience, currentCompany);
    }
}
