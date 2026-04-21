package com.portal.candidate.rest;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.Candidate;
import com.portal.candidate.domain.CandidateRepository;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final CandidateProfileService candidateProfileService;

    public CandidateController(CandidateRepository candidateRepository,
                               CandidateProfileService candidateProfileService) {
        this.candidateRepository = candidateRepository;
        this.candidateProfileService = candidateProfileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidate(@PathVariable UUID id) {
        return candidateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/apply")
    public ResponseEntity<AppliedJob> applyJob(@RequestBody Map<String, String> request) {
        UUID candidateId = UUID.fromString(request.get("candidateId"));
        UUID jobId = UUID.fromString(request.get("jobId"));
        return ResponseEntity.ok(candidateProfileService.applyJob(candidateId, jobId));
    }
}