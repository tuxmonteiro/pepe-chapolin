package com.globo.pepe.chapolin.suites;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.globo.pepe.chapolin.services.QueueRegisterServiceTests;
import com.globo.pepe.chapolin.services.RequestServiceTests;
import com.globo.pepe.chapolin.services.StackstormServiceTests;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.HttpStatus;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        StackstormServiceTests.class,
        RequestServiceTests.class,
        QueueRegisterServiceTests.class
})
public class PepeSuiteTests {

    public static ClientAndServer mockApiServer;
    public static ClientAndServer mockAuthServer;
    public static ClientAndServer mockServerRabbit;

    @BeforeClass
    public static void setup() {
        try {
            startMockserverStackstorm();
            startMockServerRabbit();
        } catch (IOException e) {
            //ignore
        }
    }

    @AfterClass
    public static void cleanup() {
        stopMockServerStackstorm();
        stopMockServerRabbit();
    }

    public static void startMockserverStackstorm() throws IOException {
        mockAuthServer = ClientAndServer.startClientAndServer(9100);
        mockApiServer = ClientAndServer.startClientAndServer(9101);

        InputStream tokenCreatedIS = PepeSuiteTests.class.getResourceAsStream("/token-created.json");
        String tokenCreated = IOUtils.toString(tokenCreatedIS, Charset.defaultCharset());
        mockAuthServer.when(request().withMethod("POST").withPath("/auth/v1/token"))
            .respond(response().withBody(tokenCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(HttpStatus.CREATED.value()));
    }

    public static void mockApiServerApiKeyCreated() throws IOException {
        InputStream tokenCreatedIS = RequestServiceTests.class.getResourceAsStream("/apikey-created.json");
        String tokenCreated = IOUtils.toString(tokenCreatedIS, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/apikey"))
            .respond(response().withBody(tokenCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(
                HttpStatus.CREATED.value()));
    }

    public static void startMockServerRabbit() {
        mockServerRabbit = ClientAndServer.startClientAndServer(15672);
    }

    public static void stopMockServerStackstorm() {
        if (mockAuthServer.isRunning()) {
            mockAuthServer.stop();
        }
        if (mockApiServer.isRunning()) {
            mockApiServer.stop();
        }
    }

    public static void stopMockServerRabbit() {
        if (mockServerRabbit.isRunning()) {
            mockServerRabbit.stop();
        }
    }
}
