package com.harshil.ragqa.controller;

import com.harshil.ragqa.dto.Dtos;
import com.harshil.ragqa.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Dtos.QueryResponse> query(@Valid @RequestBody Dtos.QueryRequest request) {
        return ResponseEntity.ok(queryService.answer(request.question()));
    }
}
