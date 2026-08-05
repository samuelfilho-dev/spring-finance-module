package com.samuelfilho_dev.finance_module.users.services.impl;

import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.entities.Address;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.mappers.UserMapper;
import com.samuelfilho_dev.finance_module.users.repositories.AddressRepository;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.users.services.UserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    private final UserMapper userMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public UserResponse createUser(CreateUserRequest payload) {
        var newUser = User.builder()
                .name(payload.name())
                .email(payload.email())
                .password(payload.password())
                .build();

        var user = userRepository.save(newUser);

        if (payload.address() != null) {
            var newAddress = Address.builder()
                    .street(payload.address().street())
                    .number(payload.address().number())
                    .complement(payload.address().complement())
                    .city(payload.address().city())
                    .state(payload.address().state())
                    .postalCode(payload.address().postalCode())
                    .userId(new ObjectId(user.getId()))
                    .build();

            addressRepository.save(newAddress);
        }

        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAllUsers() {
        var lookup = LookupOperation.newLookup()
                .from("address")
                .localField("_id")
                .foreignField("userId")
                .as("address");

        var unwind = Aggregation.unwind("address", true);
        var aggregation = Aggregation.newAggregation(lookup, unwind);
        var users = mongoTemplate.aggregate(aggregation, User.class, User.class).getMappedResults();

        return userMapper.toResponseList(users);
    }

    @Override
    public UserResponse findUserById(String id) {
        var userResponse = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(("Usuário não encontrado")));

        return userMapper.toResponse(userResponse);
    }

    @Override
    public UserResponse findUserByEmail(String email) {
        var userResponse = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NoSuchElementException(("Usuário não encontrado")));

        return userMapper.toResponse(userResponse);
    }

    @Override
    public UserResponse updateUserById(String id, UserResponse payload) {
        return null;
    }

    @Override
    public void deleteUserById(String id) {

    }

}
