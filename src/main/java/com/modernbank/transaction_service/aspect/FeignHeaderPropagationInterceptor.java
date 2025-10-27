package com.modernbank.transaction_service.aspect;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignHeaderPropagationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Şu anda geçerli HTTP isteğini al
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Authorization, Trace ID, Language vs. header’larını MCP'den backend'e ilet
            copyHeaderIfPresent(request, template, "Authorization");
            copyHeaderIfPresent(request, template, "X-User-Id");
            copyHeaderIfPresent(request, template, "X-User-Email");
            copyHeaderIfPresent(request, template, "X-User-Role");
        }
    }

    private void copyHeaderIfPresent(HttpServletRequest request, RequestTemplate template, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            template.header(headerName, headerValue);
        }
    }
}