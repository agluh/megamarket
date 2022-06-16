package com.github.agluh.megamarket.controller;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class OpenApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
            ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
            ErrorAttributeOptions.defaults());
        errorAttributes.remove("path");
        errorAttributes.remove("timestamp");
        errorAttributes.put("code", errorAttributes.get("status"));
        errorAttributes.remove("status");
        errorAttributes.put("message", errorAttributes.get("error"));
        errorAttributes.remove("error");
        return errorAttributes;
    }
}
