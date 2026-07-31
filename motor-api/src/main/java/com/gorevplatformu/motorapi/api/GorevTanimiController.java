package com.gorevplatformu.motorapi.api;

import com.gorevplatformu.motorapi.api.dto.GorevTanimiCevabi;
import com.gorevplatformu.motorspringstarter.GorevTanimiRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gorev-tanimlari")
public class GorevTanimiController {

    private final GorevTanimiRepository gorevTanimiRepository;

    public GorevTanimiController(GorevTanimiRepository gorevTanimiRepository) {
        this.gorevTanimiRepository = gorevTanimiRepository;
    }

    @GetMapping
    public List<GorevTanimiCevabi> listele() {
        return gorevTanimiRepository.findAll().stream().map(GorevTanimiCevabi::olustur).toList();
    }
}
