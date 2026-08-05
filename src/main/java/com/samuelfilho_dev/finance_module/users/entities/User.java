package com.samuelfilho_dev.finance_module.users.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

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
}
