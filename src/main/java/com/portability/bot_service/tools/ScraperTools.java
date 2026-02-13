package com.portability.bot_service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.feign.PortabilitiesInterface;
import com.portability.bot_service.feign.ScraperInterface;
import com.portability.bot_service.model.dto.ScrapePortabilityRequest;
import com.portability.bot_service.model.dto.ScrapePortabilityResponse;
import com.portability.bot_service.model.dto.ScrapeRequest;
import com.portability.bot_service.model.dto.ScrapeResponse;

import feign.FeignException;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ScraperTools {

    private final ScraperInterface scraperInterface;
    private final PortabilitiesInterface portabilitiesInterface;

    @Tool(name = "scrapeImeiCompatibility", description = "Scrape IMEI compatibility information from external service")
    public ScrapeResponse scrapeImeiCompatibility(
            @ToolParam ScrapeRequest request) {
        try {
            ResponseEntity<ScrapeResponse> response = scraperInterface.scrapeCompatibility(request);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "scrapeImeiCompatibility",
                        "No se pudo verificar la compatibilidad del IMEI. Intenta nuevamente más tarde",
                        "HTTP Status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "scrapeImeiCompatibility",
                    "El servicio de verificación de IMEI no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "scrapeImeiCompatibility",
                    "Ocurrió un error al verificar la compatibilidad del IMEI",
                    e);
        }
    }

    @Tool(name = "scrapePortability", description = """
            Scrape portability process.
            ScrapePortabilityRequest:
            - phone_number: String
            - imei: String
            - portability_nip: String
            - icc: String
            - first_name: String
            - last_name: String
            - email: String
            """)
    public ScrapePortabilityResponse scrapePortability(
            @ToolParam ScrapePortabilityRequest request) {
        try {
            ResponseEntity<ScrapePortabilityResponse> response = scraperInterface.scrapePortability(request);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "scrapePortability",
                        "No se pudo verificar la portabilidad. Intenta nuevamente más tarde",
                        "HTTP Status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "scrapePortability",
                    "El servicio de verificación de portabilidad no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "scrapePortability",
                    "Ocurrió un error al verificar la portabilidad",
                    e);
        }
    }
}
