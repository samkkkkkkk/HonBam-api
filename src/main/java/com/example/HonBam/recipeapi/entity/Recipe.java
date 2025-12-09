package com.example.HonBam.recipeapi.entity;

import lombok.*;

import javax.persistence.*;

@Getter @Setter
@ToString
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_id")
    private Long dataId;

    @Column(name = "cocktail_name")
    private String cocktailName;

    @Column(name = "cocktail_img")
    private String cocktailImg;


    @Column(columnDefinition = "TEXT")
    private String recipe;

    @Column(name = "recipe_detail", columnDefinition = "LONGTEXT")
    private String recipeDetail;

}
