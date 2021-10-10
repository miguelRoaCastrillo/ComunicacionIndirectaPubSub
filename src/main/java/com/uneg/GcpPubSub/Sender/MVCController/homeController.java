package com.uneg.GcpPubSub.Sender.MVCController;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
@Slf4j
@Controller
public class homeController {

    @GetMapping("/")
    public String home(Model model){
        log.info("Se debería de abrir la página de inicio");
        model.addAttribute("mensaje", "Hola desde el controlador");
        return "home";
    }

    @GetMapping("/error")
    public String errorPage(Model model){
        log.info("Existe une rror");
        return "error";
    }
}
