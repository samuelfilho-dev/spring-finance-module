package com.samuelfilho_dev.finance_module.launches;

import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.launches.mappers.LaunchMapper;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchAggregationRepository;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LaunchTools {

    private final LaunchRepository launchRepository;
    private final LaunchAggregationRepository aggregationRepository;

    private final LaunchMapper launchMapper;

    @Tool(description = "Calcula o saldo (receitas - despesas) em um período. Datas no formato yyyy-MM-dd")
    public String calculateAmount(String startDate, String endDate) {
        var start = LocalDate.parse(startDate);
        var end = LocalDate.parse(endDate);

        var amount = aggregationRepository.calculateAmount(start, end);
        return "O saldo entre " + start + " e " + end + " é: " + amount;
    }

    @Tool(description = "Mostra o total gasto ou recebido por categoria em um período. tipo deve ser RECEITA ou DESPESA")
    public String totalByCategory(String startDate, String endDate, String type) {
        var start = LocalDate.parse(startDate);
        var end = LocalDate.parse(endDate);

        var totals = aggregationRepository.totalByCategory(start, end, LaunchType.valueOf(type));

        if (totals.isEmpty()) return "Nenhum lançamento encontrado no período informado.";

        return totals.entrySet().stream()
                .map(e -> e.getKey() + ": R$ " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Lista lançamentos de uma categoria específica")
    public List<LaunchResponse> findByCategory(String category, String userId) {
        var launches = this.launchRepository.findByCategoryIgnoringCaseAndUserId(category, userId);
        return launchMapper.toResponseList(launches);
    }

    @Tool(description = "Lista lançamentos de um tipo (RECEITA ou DESPESA) em um período")
    public List<LaunchResponse> findByTypeAndPeriod(String type, String startDate, String endDate, String userId) {
        var start = LocalDate.parse(startDate);
        var end = LocalDate.parse(endDate);

        var launches = this.launchRepository.findByTypeAndLaunchDateBetweenAndUserId(
                LaunchType.valueOf(type), start, end, userId
        );

        return launchMapper.toResponseList(launches);
    }
}
