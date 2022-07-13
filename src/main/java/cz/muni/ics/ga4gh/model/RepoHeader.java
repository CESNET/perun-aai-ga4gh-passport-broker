package cz.muni.ics.ga4gh.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepoHeader implements ClientHttpRequestInterceptor {

    private String header;

    private String value;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(header, value);

        return execution.execute(request, body);
    }
}
