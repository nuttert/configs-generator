<h1 align="center">OpenAPI Generator</h1>

<div align="center">


<div align="center">

</div>



## Overview
Кодогенератор сервисов, ручек, клиентов, конфигов, документации.

|                                  | Languages/Frameworks                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **API clients**                  | C++ client |
| **Server stubs**                 | C++ server with POCO framework                                                                                                                                                                                         |
| **API documentation generators** | **HTML**, **Confluence Wiki**, **Asciidoc**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Configuration files**          | C++ models                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |

## Table of contents

  - [Установка](#install)
  - [Overview](#overview)

## [Установка](#install)
1. `mvn clean install -DskipTests` (Билдим базовые генератор и всё, что с ним связано)
2. Дальше сборка конкретного генератора:
- `sudo mvn clean install -f ./Generators/Handlers/ -DskipTests`
- `sudo mvn clean install -f ./Generators/Configs/ -DskipTests`
- `sudo mvn clean install -f ./Generators/Clients/ -DskipTests`
3. Конкретный генератор не может "жить" без базового, поэтому слепляем их и запускаем:
Пример для ручек
```
java -cp ./Generators/Handlers/target/handlersGenerator-openapi-generator-1.0.0.jar:modules/openapi-generator \
-cli/target/openapi-generator-cli.jar   org.openapitools.codegen.OpenAPIGenerator generate \
-g handlersGenerator  \
-i https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/2_0/petstore.yaml  \
-o ./out/myClient
```
