package dps.productionline.rest;

import dps.common.ProductionLineInfo;

import java.util.Optional;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class AdminServerClient {
    private final String serverBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public AdminServerClient(String serverAddress, int serverPort) {
        this.serverBaseUrl = "http://" + serverAddress + ":" + serverPort;
    }

    public Optional<List<ProductionLineInfo>> register(ProductionLineInfo self) {
        String url = serverBaseUrl + "/lines";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductionLineInfo> request = new HttpEntity<>(self, headers);

        try {
            ResponseEntity<List<ProductionLineInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<ProductionLineInfo>>() {
                    });
            return Optional.of(response.getBody());
        } catch (HttpClientErrorException.Conflict e) {
            return Optional.empty();
        } catch (RestClientException e) {
            System.out.println("Failed to reach admin server: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void removeLine(int lineId) {
        String urlToRemove = serverBaseUrl + "/lines/" + lineId;
        try {
            restTemplate.delete(urlToRemove);
        } catch (Exception e) {
            System.out.println("Admin server has failed to remove line " + lineId);
        }

    }
}
