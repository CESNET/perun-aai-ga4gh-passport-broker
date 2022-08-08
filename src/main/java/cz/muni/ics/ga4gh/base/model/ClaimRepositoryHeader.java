package cz.muni.ics.ga4gh.base.model;

import java.io.IOException;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimRepositoryHeader implements ClientHttpRequestInterceptor {

    @NotBlank
    private String header;

    @NotBlank
    private String value;

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution)
        throws IOException
    {
        request.getHeaders().add(header, value);
        return execution.execute(request, body);
    }

}
