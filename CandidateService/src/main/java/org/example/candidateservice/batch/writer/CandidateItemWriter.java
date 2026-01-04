package org.example.candidateservice.batch.writer;

import org.example.candidateservice.entity.Candidate;
import org.example.candidateservice.repository.CandidateRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateItemWriter implements ItemWriter<Candidate> {

    private final CandidateRepository candidateRepository;

    @Autowired
    public CandidateItemWriter(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Override
    public void write(List<? extends Candidate> candidates) throws Exception {
        // Save all candidates in the list to the database
        // The repository's saveAll method handles batch inserts/updates efficiently
        candidateRepository.saveAll(candidates);
        System.out.println("Wrote " + candidates.size() + " candidates to the database.");
    }
}
