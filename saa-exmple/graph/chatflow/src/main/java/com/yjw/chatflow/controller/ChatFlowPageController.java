package com.yjw.chatflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatFlowPageController {

    @GetMapping({"/", "/chat"})
    public String chatPage(Model model) {
        model.addAttribute("pageTitle", "ChatFlow 待办助手");
        model.addAttribute("defaultSessionId", "yjw");
        return "chat";
    }
}
