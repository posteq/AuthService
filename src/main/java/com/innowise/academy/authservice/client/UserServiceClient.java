package com.innowise.academy.authservice.client;


import com.innowise.academy.authservice.dto.client.request.UserProfileRequest;
import com.innowise.academy.authservice.dto.client.response.UserProfileResponse;
import com.innowise.academy.authservice.exception.UserProfileConflictException;
import com.innowise.academy.authservice.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestClient restClient;

    public Long createUser(UserProfileRequest request){
        UserProfileResponse response ;
        try {
            response = restClient.post()
                    .uri("/api/users")
                    .body(request)
                    .retrieve()
                    .body(UserProfileResponse.class);
        }catch (RestClientResponseException e){
            if(e.getStatusCode().value() == HttpStatus.CONFLICT.value()){
                throw new UserProfileConflictException("User profile already exists");
            }
            throw new UserServiceException("User Service is unavailable");
        }catch (RestClientException e) {
            throw new UserServiceException("User Service is unavailable");
        }
        if (response == null || response.id() == null){
            throw new UserServiceException("User Service returned no user id");
        }
        return response.id();
    }

    public void deleteUser(Long userId){
        try {
            restClient.delete()
                    .uri("/api/users/{userId}",userId)
                    .retrieve()
                    .toBodilessEntity();
        }catch (RestClientException e){
            throw new UserServiceException("Failed to delete user profile " + userId);
        }
    }
}
