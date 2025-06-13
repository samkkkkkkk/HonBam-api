package com.example.HonBam.recipeapi.api;

import com.example.HonBam.recipeapi.dto.response.RecipeDetailResponseDTO;
import com.example.HonBam.recipeapi.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/recipe")
@CrossOrigin
public class RecipeController {

    private final RecipeService recipeService;

    // 리스트 가져오기
    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<RecipeDetailResponseDTO> allRecipes = recipeService.getAllRecipes();
            return ResponseEntity.ok().body(allRecipes);
        } catch (Exception e) {
            e.printStackTrace();
//            log.error("Error fetching recipes", e);
            return  ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String name) {
        try {
            List<RecipeDetailResponseDTO> searchResults = recipeService.searchRecipes(name);
            return ResponseEntity.ok().body(searchResults);
        } catch (Exception e) {
            e.printStackTrace();
//            log.error("Error searching recipes", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }
}