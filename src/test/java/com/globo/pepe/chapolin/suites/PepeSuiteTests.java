package com.globo.pepe.chapolin.suites;

import com.globo.pepe.chapolin.services.QueueRegisterServiceTests;
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
        RequestServiceTests.class,
        QueueRegisterServiceTests.class
})
public class PepeSuiteTests {

    public static ClientAndServer mockServer;
    public static ClientAndServer mockServerRabbit;

    @BeforeClass
    public static void setup() {
        startMockserver();
        startMockServerRabbit();
    }

    @AfterClass
    public static void cleanup() {
        stopMockServer();
        stopMockServerRabbit();
    }

    public static void startMockserver() {
        mockServer = ClientAndServer.startClientAndServer(9101);
    }

    public static void startMockServerRabbit() {
        mockServerRabbit = ClientAndServer.startClientAndServer(15672);
    }

    public static void stopMockServer() {
        if (mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    public static void stopMockServerRabbit() {
        if (mockServerRabbit.isRunning()) {
            mockServerRabbit.stop();
        }
    }
}
