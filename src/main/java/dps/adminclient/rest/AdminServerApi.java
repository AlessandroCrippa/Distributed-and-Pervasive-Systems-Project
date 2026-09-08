package dps.adminclient.rest;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dps.common.OperationalState;
import dps.common.ProductionLineInfo;

import java.util.List;
import java.util.Optional;

public class AdminServerApi {
    private final String url;
    private final RestTemplate restTemplate = new RestTemplate();

    public AdminServerApi(String address, int port) {
        this.url = "http://" + address + ":" + port;
    }

    public List<ProductionLineInfo> getAllLines() {
        try {
            ProductionLineInfo[] lines = restTemplate.getForObject(url + "/lines", ProductionLineInfo[].class);
            return List.of(lines);
        } catch (RestClientException e) {
            System.out.println("Failed to reach admin server: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<OperationalState> getState(int lineId) {
        try {
            OperationalState state = restTemplate.getForObject(url + "/lines/" + lineId + "/state",
                    OperationalState.class);
            return Optional.ofNullable(state);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            System.out.println("Failed to reach admin server: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Double> getAverage(int lineId, long from, long to) {
        String urlAvg = url + "/lines/" + lineId + "/average?from=" + from + "&to=" + to;
        try {
            Double avg = restTemplate.getForObject(urlAvg, Double.class);
            return Optional.ofNullable(avg);

        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            System.out.println("Failed to reach admin server: " + e.getMessage());
            return Optional.empty();
        }
    }
}
