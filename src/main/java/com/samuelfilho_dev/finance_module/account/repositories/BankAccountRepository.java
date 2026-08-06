package com.samuelfilho_dev.finance_module.account.repositories;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

}
