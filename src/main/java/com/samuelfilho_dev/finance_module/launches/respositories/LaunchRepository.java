package com.samuelfilho_dev.finance_module.launches.respositories;

import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaunchRepository extends MongoRepository<Launch, String> {
    boolean existsByFitIdAndUserId(String fitId, String userId);
}
