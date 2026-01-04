package org.example.candidateservice.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import org.example.candidateservice.batch.dto.PartnerCandidateRecord;
import org.example.candidateservice.entity.Candidate;

@Configuration
public class CandidateBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ItemReader<PartnerCandidateRecord> candidateItemReader;
    private final ItemProcessor<PartnerCandidateRecord, Candidate> candidateItemProcessor;
    private final ItemWriter<Candidate> candidateItemWriter;

    @Autowired
    public CandidateBatchJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<PartnerCandidateRecord> candidateItemReader,
            ItemProcessor<PartnerCandidateRecord, Candidate> candidateItemProcessor,
            ItemWriter<Candidate> candidateItemWriter) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.candidateItemReader = candidateItemReader;
        this.candidateItemProcessor = candidateItemProcessor;
        this.candidateItemWriter = candidateItemWriter;
    }

    @Bean
    public Job importCandidatesJob() {
        return new JobBuilder("importCandidatesJob", jobRepository)
                .start(candidateImportStep())
                .build();
    }

    @Bean
    public Step candidateImportStep() {
        // Define chunk size for processing
        int chunkSize = 10; // Process 10 records at a time

        return new StepBuilder("candidateImportStep", jobRepository)
                .<PartnerCandidateRecord, Candidate>chunk(chunkSize, transactionManager)
                .reader(candidateItemReader)
                .processor(candidateItemProcessor)
                .writer(candidateItemWriter)
                // Optional: Configure listeners for logging, error handling, etc.
                // .listener(...)
                .build();
    }
}
