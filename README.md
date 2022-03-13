## Overview
Проект содержит кодогенераторы:
- кодогенератор моделей и коллекций для MongoDB.
- кодогенератор конфигов

## [Установка и запуск](#install)
1. Собираем базовые генератор:
- `mvn clean install -DskipTests`
2. Дальше сборка конкретного генератора:
- `sudo mvn clean install -f ./Generators/Models/ -DskipTests`
- `sudo mvn clean install -f ./Generators/Configs/ -DskipTests`
3. Конкретный генератор не может существовать без базового. 
Команда генерации для моделей:
```
java -cp ./Generators/Models/target/modelsGenerator-1.0.0.jar:modules/openapi-generator \
-cli/target/openapi-generator-cli.jar   org.openapitools.codegen.OpenAPIGenerator generate \
-g modelsGenerator  \
-i https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/2_0/petstore.yaml  \
-o ./out/models
```
Команда генерации для конфигов:
```
java -cp ./Generators/Configs/target/configsGenerator-1.0.0.jar:modules/openapi-generator \
-cli/target/openapi-generator-cli.jar   org.openapitools.codegen.OpenAPIGenerator generate \
-g configsGenerator  \
-i https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/2_0/petstore.yaml  \
-o ./out/configs
```
