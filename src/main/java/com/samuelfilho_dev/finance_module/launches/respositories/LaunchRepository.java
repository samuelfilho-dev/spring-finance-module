package com.samuelfilho_dev.finance_module.launches.respositories;

import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LaunchRepository extends MongoRepository<Launch, String> {

    boolean existsByFitIdAndUserId(String fitId, String userId);

    List<Launch> findByCategoryIgnoringCaseAndUserId(String category, String userId);

    List<Launch> findByTypeAndLaunchDateBetweenAndUserId(LaunchType type, LocalDate startDate, LocalDate endDate, String userId);
}
