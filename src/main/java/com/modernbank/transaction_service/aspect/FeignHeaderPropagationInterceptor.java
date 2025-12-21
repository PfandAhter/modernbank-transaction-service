package com.modernbank.transaction_service.aspect;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.modernbank.transaction_service.constant.HeaderKey.*;

@Component
public class FeignHeaderPropagationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            copyHeaderIfPresent(request, template, AUTHORIZATION_TOKEN);
            copyHeaderIfPresent(request, template, USER_ID);
            copyHeaderIfPresent(request, template, USER_EMAIL);
            copyHeaderIfPresent(request, template, USER_ROLE);
            copyHeaderIfPresent(request, template, CORRELATION_ID);
        }
    }

    private void copyHeaderIfPresent(HttpServletRequest request, RequestTemplate template, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            template.header(headerName, headerValue);
        }
    }
}