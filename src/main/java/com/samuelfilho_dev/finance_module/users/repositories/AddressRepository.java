package com.samuelfilho_dev.finance_module.users.repositories;

import com.samuelfilho_dev.finance_module.users.entities.Address;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends MongoRepository<Address, String> {
}
