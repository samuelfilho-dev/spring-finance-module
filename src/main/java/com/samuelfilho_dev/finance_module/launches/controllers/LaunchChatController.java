package com.samuelfilho_dev.finance_module.launches.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{version}/launches/chat", version = "1")
public class LaunchChatController {

    private final ChatClient chatClient;

    @GetMapping
    public String chat(@RequestParam String question) {
        return chatClient.prompt().user(question).call().content();
    }
}
