package com.samuelfilho_dev.finance_module.launches.respositories;

import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LaunchAggregationRepository {
    private final MongoTemplate mongoTemplate;

    public BigDecimal calculateAmount(LocalDate startDate, LocalDate endDate) {
        var match = Aggregation.match(
                Criteria.where("launchDate").gte(startDate).lte(endDate)
        );

        var group = Aggregation.group("type").sum("amount").as("totalAmount");

        var result = mongoTemplate.aggregate(
                Aggregation.newAggregation(match, group),
                "launches",
                Document.class
        );

        var totals = result.getMappedResults().stream()
                .collect(Collectors.toMap(
                        doc -> doc.getString("_id"),
                        doc -> new BigDecimal(doc.getString("totalAmount"))
                ));

        var recipes = totals.getOrDefault("RECIPE", BigDecimal.ZERO);
        var expenses = totals.getOrDefault("EXPENSE", BigDecimal.ZERO);

        return recipes.subtract(expenses);
    }

    public Map<String, BigDecimal> totalByCategory(LocalDate startDate, LocalDate endDate, LaunchType type) {
        var match = Aggregation.match(
                Criteria.where("launchDate").gte(startDate).lte(endDate).and("type").is(type.name())
        );

        var group = Aggregation.group("category").sum("amount").as("totalAmount");

        var result = mongoTemplate.aggregate(
                Aggregation.newAggregation(match, group),
                "launches",
                Document.class
        );

        return result.getMappedResults().stream()
                .collect(Collectors.toMap(
                        doc -> doc.getString("_id"),
                        doc -> new BigDecimal(doc.getString("totalAmount"))
                ));
    }
}
