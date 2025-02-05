package com.example.HonBam.recipeapi.service;

import com.example.HonBam.recipeapi.dto.response.RecipeDetailResponseDTO;
import com.example.HonBam.recipeapi.entity.Recipe;
import com.example.HonBam.recipeapi.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public List<RecipeDetailResponseDTO> getAllRecipes() {
        List<Recipe> findList = recipeRepository.findAll();
//        log.info("레시피: {}", findList);
        return findList.stream().map(RecipeDetailResponseDTO::new).collect(Collectors.toList());
    }

    public List<RecipeDetailResponseDTO> searchRecipes(String name) {
        List<Recipe> searchResults = recipeRepository.findByCocktailNameContainingIgnoreCase(name);
//        log.info("검색한 결과 {}", searchResults);
        return searchResults.stream().map(RecipeDetailResponseDTO::new).collect(Collectors.toList());
    }
}