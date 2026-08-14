package com.samuelfilho_dev.finance_module.users.entities;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    private String id;

    private String name;

    private String email;

    private String password;

    @CreatedDate
    private Instant createAt;

    @LastModifiedDate
    private Instant updateAt;

    @Field
    private Address address;

    @Field
    private List<BankAccount> accounts;
}
