package com.example.HonBam.recipeapi.dto.response;

import com.example.HonBam.recipeapi.entity.Recipe;
import lombok.*;

@Setter @Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeDetailResponseDTO {

    private Long dataId;

    private String cocktailName;

    private String cocktailImg;

    private String recipe;

    private String recipeDetail;

    public RecipeDetailResponseDTO(Recipe recipe) {
        this.dataId =recipe.getDataId();
        this.cocktailName = recipe.getCocktailName();
        this.cocktailImg = recipe.getCocktailImg();
        this.recipe = recipe.getRecipe();
        this.recipeDetail = recipe.getRecipeDetail();
    }
}