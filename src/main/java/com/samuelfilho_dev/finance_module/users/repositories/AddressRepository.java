package com.samuelfilho_dev.finance_module.users.repositories;

import com.samuelfilho_dev.finance_module.users.entities.Address;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends MongoRepository<Address, String> {
    Optional<Address> findByUserId(ObjectId userId);
}
