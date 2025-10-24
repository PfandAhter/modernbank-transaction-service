package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.constant.HeaderKey;
import com.modernbank.transaction_service.service.HeaderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeaderServiceImpl implements HeaderService {

    private final HttpServletRequest request;

    @Override
    public String extractToken(){
        String token = null;
        try{
            token = request.getHeader(HeaderKey.AUTHORIZATION_TOKEN);
        }catch (Exception E){
            log.error("Error while extracting token from header: {}", E.getMessage());
        }
        return token;
    }

    @Override
    public String extractUserEmail(){
        String userEmail = null;
        try{
            userEmail = request.getHeader(HeaderKey.USER_EMAIL);
        }catch(Exception e){
            log.error("Error while extracting userEmail from token: {}", e.getMessage());
        }
        return userEmail;
    }

    @Override
    public String extractUserId(){
        String userId = null;
        try{
            userId = request.getHeader(HeaderKey.USER_ID);
        }catch (Exception E){
            log.error("Error while extracting userId from token: {}", E.getMessage());
        }
        return userId;
    }

    @Override
    public String extractUserRole(){
        String userRole = null;
        try{
            userRole = request.getHeader(HeaderKey.USER_ROLE);
        }catch (Exception e){
            log.error("Error while extracting userRole from token: {}", e.getMessage());
        }
        return userRole;
    }
}
