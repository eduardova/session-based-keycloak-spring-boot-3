package dev.eduardova.secexample.controllers;

import jakarta.annotation.security.RolesAllowed;
import java.security.Principal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.access.annotation.Secured;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("/home")
    @RolesAllowed("admin") // or:
//    @Secured("ROLE_admin")
//    @PreAuthorize("hasRole('ROLE_admin')")
    public String home(Model model, Principal usr) {

        var email = ((OAuth2AuthenticationToken) usr).getPrincipal().getAttribute("email");

        model.addAttribute("props", System.getProperties());
        model.addAttribute("user", usr.getName());
        model.addAttribute("email", email);
        return "hello";
    }

    @GetMapping("/")
    public String homeAnonymus(Model model) {
        model.addAttribute("props", System.getProperties());
        return "hello";
    }
}
