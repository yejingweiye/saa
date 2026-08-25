package com.yjw.chatflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 待办助手聊天页面控制器，负责渲染 ChatFlow 首页视图。
 */
@Controller
public class ChatFlowPageController {

    /**
     * 进入聊天页面，向视图填充页面标题与默认会话 ID。
     *
     * @param model Spring MVC 视图模型，用于向模板传递属性
     * @return 视图名称 {@code chat}
     */
    @GetMapping({"/", "/chat"})
    public String chatPage(Model model) {
        model.addAttribute("pageTitle", "ChatFlow 待办助手");
        model.addAttribute("defaultSessionId", "yjw");
        return "chat";
    }
}
