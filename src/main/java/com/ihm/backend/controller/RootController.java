package com.ihm.backend.controller;

import com.ihm.backend.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, String>>> index() {
        Map<String, String> data = new HashMap<>();
        data.put("api_name", "XCCM1 API");
        data.put("version", "v1");
        data.put("status", "UP");

        return ResponseEntity.ok(ApiResponse.success("Bienvenue sur l'API XCCM1", data));
    }
}
