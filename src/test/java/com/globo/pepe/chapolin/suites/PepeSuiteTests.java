package com.globo.pepe.chapolin.suites;

import com.globo.pepe.chapolin.services.RequestServiceTests;
import com.globo.pepe.chapolin.services.StackstormServiceTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.mockserver.integration.ClientAndServer;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        StackstormServiceTests.class,
        RequestServiceTests.class
})
public class PepeSuiteTests {

    public static ClientAndServer mockServer;

    @BeforeClass
    public static void setup() {
        startMockserver();
    }

    @AfterClass
    public static void cleanup() {
        stopMockServer();
    }

    public static void startMockserver() {
        mockServer = ClientAndServer.startClientAndServer(9101);
    }

    public static void stopMockServer() {
        if (mockServer.isRunning()) {
            mockServer.stop();
        }
    }
}
