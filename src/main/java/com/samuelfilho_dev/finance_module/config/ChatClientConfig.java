package com.samuelfilho_dev.finance_module.config;

import com.samuelfilho_dev.finance_module.launches.LaunchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, LaunchTools tools) {
        return builder
                .defaultSystem("""
                        Você é um assistente financeiro. Responda perguntas sobre
                        lançamentos, saldo, receitas e despesas usando as ferramentas
                        disponíveis. Sempre que possível, traga valores em reais (R$)
                        formatados e cite o período consultado.
                        Se a pergunta envolver período e o usuário não especificar,
                        assuma o mês atual.
                        Nunca invente números — use sempre os dados retornados pelas ferramentas.
                        """)
                .defaultTools(tools)
                .build();
    }
}
