package com.portability.bot_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.portability.bot_service.model.dto.ScrapePortabilityRequest;
import com.portability.bot_service.model.dto.ScrapePortabilityResponse;
import com.portability.bot_service.model.dto.ScrapeRequest;
import com.portability.bot_service.model.dto.ScrapeResponse;

@FeignClient(name = "SCRAPER-SERVICE")
@Component
public interface ScraperInterface {

    @PostMapping("api/scrape-compatibility")
    public ResponseEntity<ScrapeResponse> scrapeCompatibility(
            @RequestBody ScrapeRequest requestBody);

    @PostMapping("api/scrape-portability")
    public ResponseEntity<ScrapePortabilityResponse> scrapePortability(
            @RequestBody ScrapePortabilityRequest requestBody);
}
