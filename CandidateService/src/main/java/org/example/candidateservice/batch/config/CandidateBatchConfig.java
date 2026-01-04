package org.example.candidateservice.batch.config;

import org.example.candidateservice.batch.dto.PartnerCandidateRecord;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class CandidateBatchConfig {

  @Bean
  public FlatFileItemReader<PartnerCandidateRecord> candidateItemReader() {
    return new FlatFileItemReaderBuilder<PartnerCandidateRecord>()
      .name("candidateItemReader")
      // Assuming the CSV file is named 'partner_candidates.csv'
      // and located in src/main/resources/data/
      .resource(new ClassPathResource("data/partner_candidates.csv"))
      .delimited()
      .names(
        new String[] { // These names must match the fields in PartnerCandidateRecord
          "email",
          "firstName",
          "lastName",
          "phone",
          "primarySkill",
          "yearsOfExperience",
          "currentCompany",
        }
      )
      .fieldSetMapper(
        new BeanWrapperFieldSetMapper<PartnerCandidateRecord>() {
          { // Use initializer block for cleaner syntax
            setTargetType(PartnerCandidateRecord.class);
          }
        }
      )
      .linesToSkip(1) // Skip the header row
      .build();
  }
}
