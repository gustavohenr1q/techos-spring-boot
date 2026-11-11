package br.com.bugnacio.techos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import br.com.bugnacio.techos.repository.ClienteRepository;

@Controller

public class HomeController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/")

    public String inicio(Model model) {
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalEquipamentos", 10);
        model.addAttribute("osAbertas", 3);
        model.addAttribute("osConcluidas", 25);
        model.addAttribute("faturamento", 4850);
        return "dashboard";
    }

}
