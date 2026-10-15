package com.trackflow.bootstrap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La raíz no sirve ningún recurso porque esto es una API. Se redirige a la
 * documentación para que quien abra la URL encuentre por dónde empezar.
 */
@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/swagger-ui.html";
    }
}
