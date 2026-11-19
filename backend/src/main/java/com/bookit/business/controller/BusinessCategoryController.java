package com.bookit.business.controller;

import com.bookit.business.dto.BusinessCategoryResponse;
import com.bookit.business.service.BusinessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Business categories")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class BusinessCategoryController {

    private final BusinessService businessService;

    @GetMapping(version = "1")
    public List<BusinessCategoryResponse> listCategories() {
        return businessService.getCategories();
    }
}
