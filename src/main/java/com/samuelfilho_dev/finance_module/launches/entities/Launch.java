package com.samuelfilho_dev.finance_module.launches.entities;

import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "launches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Launch {
    @Id
    private String id;

    private String title;

    private String description;

    private Instant launchDate;

    private BigDecimal amount;

    private LaunchType type;

    private ObjectId userId;

    private ObjectId bankAccountId;

    @CreatedDate
    private Instant createAt;

    @LastModifiedDate
    private Instant updateAt;
}
