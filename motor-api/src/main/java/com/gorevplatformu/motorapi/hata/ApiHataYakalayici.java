package com.gorevplatformu.motorapi.hata;

import com.gorevplatformu.motorspringstarter.GorevBulunamadiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Boot'un yerlesik ProblemDetail destegi (spring.mvc.problemdetails.enabled=true)
 * framework seviyesi istisnalari (validation, tip uyusmazligi vb.) zaten otomatik RFC 7807
 * govdesine ceviriyor. Burada sadece domain istisnalarimizin HTTP status eslemesi var.
 */
@RestControllerAdvice
public class ApiHataYakalayici {

    private static final Logger log = LoggerFactory.getLogger(ApiHataYakalayici.class);

    @ExceptionHandler(GorevBulunamadiException.class)
    public ProblemDetail bulunamadi(GorevBulunamadiException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail gecersizGirdi(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail gecersizDurum(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail beklenmeyenHata(Exception e) {
        log.error("Beklenmeyen API hatasi", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata olustu");
    }
}
