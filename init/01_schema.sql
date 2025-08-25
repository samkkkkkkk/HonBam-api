USE `honbam`;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS tbl_recipe (
  data_id       INT NOT NULL AUTO_INCREMENT,
  cocktail_img  VARCHAR(1024)  NULL,
  cocktail_name VARCHAR(255)   NOT NULL,
  recipe        TEXT           NULL,
  recipe_detail LONGTEXT       NULL,
  PRIMARY KEY (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
