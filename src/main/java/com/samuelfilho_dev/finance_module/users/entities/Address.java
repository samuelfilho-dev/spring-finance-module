package com.samuelfilho_dev.finance_module.users.entities;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "address")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    @Id
    public String id;

    public String street;

    public String number;

    public String complement;

    public String city;

    public String state;

    public String postalCode;

    public ObjectId userId;
}
