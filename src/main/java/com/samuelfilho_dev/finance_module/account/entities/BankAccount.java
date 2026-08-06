package com.samuelfilho_dev.finance_module.account.entities;

import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "bankAccounts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankAccount {
    @Id
    private String id;

    private String bankName;

    private String agency;

    private String accountNumber;

    private BigDecimal balance;

    private BankAccountStatus status;

    public ObjectId userId;

    @CreatedDate
    private Instant createAt;

    @LastModifiedDate
    private Instant updateAt;
}
