package com.innowise.academy.authservice.client;

import com.innowise.academy.authservice.dto.client.request.UserProfileRequest;
import com.innowise.academy.authservice.exception.UserServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;


class UserServiceClientTest {

    private static final UserProfileRequest PROFILE = new UserProfileRequest("Ann", "Lee",
            LocalDate.of(1990, 1, 1), "ann@mail.com");
    public static final String URL = "http://user-service/api/users";

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserServiceClient(builder.build());
    }

    @Test
    void createUser_sendsTheProfile_andReturnsTheNewId() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.email").value("ann@mail.com"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":42,\"firstname\":\"Ann\",\"active\":true}"));

        assertThat(client.createUser(PROFILE)).isEqualTo(42L);
        server.verify();
    }

    @Test
    void createUser_noConnection_becomesUserServiceExceptionWithStatusZero() {
        server.expect(requestTo(URL))
                .andRespond(request -> {
                    throw new IOException("connection refused");
                });

        assertThatThrownBy(() -> client.createUser(PROFILE))
                .isInstanceOf(UserServiceException.class);
    }

    @Test
    void deleteUser_callsDelete() {
        server.expect(requestTo(URL+"/42"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        client.deleteUser(42L);

        server.verify();
    }
}
