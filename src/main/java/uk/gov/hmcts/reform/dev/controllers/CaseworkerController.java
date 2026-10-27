package uk.gov.hmcts.reform.dev.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.dev.dto.CaseworkerResponse;
import uk.gov.hmcts.reform.dev.services.CaseworkerService;

@RestController
@RequestMapping("/caseworkers")
public class CaseworkerController {

    private final CaseworkerService caseworkerService;

    public CaseworkerController(CaseworkerService caseworkerService) {
        this.caseworkerService = caseworkerService;
    }

    @GetMapping
    public List<CaseworkerResponse> getCaseworkers() {
        return caseworkerService.getCaseworkers()
            .stream()
            .map(CaseworkerResponse::from)
            .toList();
    }
}
