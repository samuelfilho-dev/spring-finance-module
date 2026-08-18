package com.samuelfilho_dev.finance_module.users.services.impl;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthPreTokenResponse;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import com.samuelfilho_dev.finance_module.auth.services.MfaFactorService;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UpdateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.entities.Address;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.mappers.UserMapper;
import com.samuelfilho_dev.finance_module.users.repositories.AddressRepository;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.users.services.UserService;
import com.samuelfilho_dev.finance_module.utils.AESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    private final UserMapper userMapper;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaFactorService mfaFactorService;
    private final AESService aesService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthPreTokenResponse createUser(CreateUserRequest payload) {
        if (userRepository.existsByEmail(payload.email())) {
            throw new BusinessException("Email já cadastrado");
        }

        var secret = aesService.encrypt(mfaFactorService.generateSecret());

        var newUser = User.builder()
                .name(payload.name())
                .email(payload.email())
                .mfaSecret(secret)
                .password(passwordEncoder.encode(payload.password()))
                .build();

        var user = userRepository.save(newUser);

        log.info("Usuario foi criado: {}", user);

        var otpAuthUrl = mfaFactorService.buildOtpAuthUrl(user.getEmail(), secret);
        var qrCodeBase64 = mfaFactorService.generateQrCodeImageBase64(user.getEmail(), secret);
        var setupToken = jwtService.generateSetupToken(user);

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

            log.info("Endereco foi criado: {}", newAddress);

            addressRepository.save(newAddress);
        }

        return new AuthPreTokenResponse(
                true,
                "2FA é obrigatório. Escaneie o QR Code enviado",
                "/api/v1/auth/mfa/enable",
                setupToken,
                qrCodeBase64,
                otpAuthUrl
        );
    }

    @Override
    public List<UserResponse> findAllUsers() {
        log.info("Listando todos os usuarios");

        var lookup = createAddressLookup();
        var unwind = Aggregation.unwind("address", true);
        var aggregation = Aggregation.newAggregation(lookup, unwind);
        var users = mongoTemplate.aggregate(aggregation, User.class, User.class).getMappedResults();

        return userMapper.toResponseList(users);
    }

    @Override
    public UserResponse findUserById(String id) {
        log.info("Buscando o usuario pelo id: {}", id);

        var addressLookup = createAddressLookup();
        var accountsLookup = LookupOperation.newLookup()
                .from("bankAccounts")
                .localField("_id")
                .foreignField("userId")
                .as("accounts");

        var unwind = Aggregation.unwind("address", true);
        var aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("id").is(id)),
                addressLookup,
                accountsLookup,
                unwind
        );

        var user = mongoTemplate.aggregate(aggregation, User.class, User.class)
                .getMappedResults()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse findUserByEmail(String email) {
        log.info("Buscando o usuario pelo email: {}", email);

        var userResponse = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        return userMapper.toResponse(userResponse);
    }

    @Override
    public UserResponse updateUserById(String id, UpdateUserRequest payload) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        user.setName(payload.name());
        user.setEmail(payload.email());
        userRepository.save(user);

        log.info("Usuario {} foi modificado: {}", id, user);

        if (payload.address() != null) {
            var userId = new ObjectId(user.getId());
            var address = addressRepository.findByUserId(userId)
                    .orElseGet(() -> Address.builder().userId(userId).build());

            address.setStreet(payload.address().street());
            address.setNumber(payload.address().number());
            address.setComplement(payload.address().complement());
            address.setCity(payload.address().city());
            address.setState(payload.address().state());
            address.setPostalCode(payload.address().postalCode());

            addressRepository.save(address);

            log.info("Endereco com id {} foi modificado ou criado: {}", address.id, address);
        }

        return this.findUserById(id);
    }

    @Override
    public void deleteUserById(String id) {
        var user = this.findUserById(id);

        if (user.address() != null) {
            addressRepository.deleteById(user.address().id());
            log.info("Endereco com id {} foi deletado: {}", user.address().id(), user.address());
        }

        userRepository.deleteById(user.id());
        log.info("Usuario {} foi deletado: {}", id, user);
    }

    private LookupOperation createAddressLookup() {
        return LookupOperation.newLookup()
                .from("address")
                .localField("_id")
                .foreignField("userId")
                .as("address");
    }
}
