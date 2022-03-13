package Models;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.swagger.models.properties.*;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.io.*;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractCppCodegen;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.templating.mustache.*;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelsGeneratorGenerator
    extends AbstractCppCodegen implements CodegenConfig {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(DefaultCodegen.class);
  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";

  public static final String DECLSPEC = "declspec";
  public static final String DEFAULT_INCLUDE = "defaultInclude";
  public static final String GENERATE_GMOCKS_FOR_APIS = "generateGMocksForApis";

  protected String packageVersion = "1.0.0";
  protected String declspec = "";
  protected String defaultInclude = "";
  protected String requestsFolder = "Requests";
  protected String responsesFolder = "Responses";
  protected String implFolder = "Impl";

  private final Set<String> parentModels = new HashSet<>();
  private final Multimap<String, CodegenModel> childrenByParent =
      ArrayListMultimap.create();

  public class SimpleModel {
    String name;
  };
  public class SimpleConfig {
    String name;
  };
  /**
   * Configures the type of generator.
   *
   * @return  the CodegenType for this generator
   * @see     org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() { return CodegenType.OTHER; }

  /**
   * Configures a friendly name for the generator.  This will be used by the
   * generator to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() { return "modelsGenerator"; }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with
   * help tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a modelsGenerator client library.";
  }

  public ModelsGeneratorGenerator() {
    super();

    modifyFeatureSet(
        features
        -> features.includeDocumentationFeatures(DocumentationFeature.Readme)
               .securityFeatures(EnumSet.of(SecurityFeature.BasicAuth,
                                            SecurityFeature.OAuth2_Implicit,
                                            SecurityFeature.ApiKey))
               .excludeGlobalFeatures(
                   GlobalFeature.XMLStructureDefinitions,
                   GlobalFeature.Callbacks, GlobalFeature.LinkObjects,
                   GlobalFeature.ParameterStyling, GlobalFeature.MultiServer)
               .includeSchemaSupportFeatures(SchemaSupportFeature.Polymorphism)
               .excludeParameterFeatures(ParameterFeature.Cookie));

    // set the output folder here
    outputFolder = "generated-code/modelsGenerator";

    modelPackage = "models";
    Map<String, String> env = System.getenv();
    String IMPL_FOLDER = env.get("IMPL_FOLDER");

    implFolder = IMPL_FOLDER;
    modelTemplateFiles.put("Models/model-header.mustache", ".hpp");
    modelTemplateFiles.put("Models/model-source.mustache", ".cpp");
    modelTemplateFiles.put("Collections/model-header.mustache",
                           "Collection.hpp");
    modelTemplateFiles.put("Collections/model-source.mustache",
                           "Collection.cpp");

    embeddedTemplateDir = templateDir = "modelsGenerator";

    cliOptions.clear();

    // CLI options
    addOption(CodegenConstants.MODEL_PACKAGE,
              "C++ namespace for models (convention: name.space.model).",
              this.modelPackage);
    addOption(CodegenConstants.API_PACKAGE,
              "C++ namespace for apis (convention: name.space.api).",
              this.apiPackage);
    addOption(CodegenConstants.PACKAGE_VERSION, "C++ package version.",
              this.packageVersion);
    addOption(
        DECLSPEC,
        "C++ preprocessor to place before the class name for handling dllexport/dllimport.",
        this.declspec);
    addOption(
        DEFAULT_INCLUDE,
        "The default include statement that should be placed in all headers for including things like the declspec (convention: #include \"Commons.h\" ",
        this.defaultInclude);
    addOption(GENERATE_GMOCKS_FOR_APIS,
              "Generate Google Mock classes for APIs.", null);
    addOption(RESERVED_WORD_PREFIX_OPTION, RESERVED_WORD_PREFIX_DESC,
              this.reservedWordPrefix);
    addOption(VARIABLE_NAME_FIRST_CHARACTER_UPPERCASE_OPTION,
              VARIABLE_NAME_FIRST_CHARACTER_UPPERCASE_DESC,
              Boolean.toString(this.variableNameFirstCharacterUppercase));

    Optional<String> api_check_o = null;
    String fileApi = ".temp/__env_api__.txt";
    try {
      api_check_o = Files.lines(Paths.get(fileApi)).findFirst();
    } catch (IOException e) {
      LOGGER.error(
          "Cannot read .temp/__env_api_names__ or .temp/__env_api_paths__ file!(the file for handlers names saving)");
      throw new RuntimeException("Could not open __env___file ", e);
    }

    languageSpecificPrimitives = new HashSet<String>(
        Arrays.asList("int", "char", "bool", "long", "float", "double",
                      "int32_t", "int64_t", "uint32_t", "uint64_t"));

    typeMapping = new HashMap<String, String>();
    typeMapping.put("string", "std::string");
    typeMapping.put("integer", "int32_t");
    typeMapping.put("long", "int64_t");
    typeMapping.put("uinteger", "uint32_t");
    typeMapping.put("ulong", "uint64_t");
    typeMapping.put("boolean", "bool");
    typeMapping.put("array", "std::vector");
    typeMapping.put("map", "std::map");
    typeMapping.put("object", "Object");
    typeMapping.put("binary", "std::string");
    typeMapping.put("number", "double");
    typeMapping.put("URI", "std::string");
    typeMapping.put("ByteArray", "std::string");

    super.importMapping = new HashMap<String, String>();
    importMapping.put("std::vector", "#include <vector>");
    importMapping.put("std::map", "#include <map>");
    importMapping.put("std::string", "#include <string>");
  }

  @Override
  public void processOpts() {
    super.processOpts();

    if (additionalProperties.containsKey(DECLSPEC)) {
      declspec = additionalProperties.get(DECLSPEC).toString();
    }

    if (additionalProperties.containsKey(DEFAULT_INCLUDE)) {
      defaultInclude = additionalProperties.get(DEFAULT_INCLUDE).toString();
    }

    if (additionalProperties.containsKey(RESERVED_WORD_PREFIX_OPTION)) {
      reservedWordPrefix =
          (String)additionalProperties.get(RESERVED_WORD_PREFIX_OPTION);
    }

    if (convertPropertyToBoolean(GENERATE_GMOCKS_FOR_APIS)) {
      apiTemplateFiles.put("api-gmock.mustache", "GMock.h");
      additionalProperties.put("gmockApis", "true");
    }

    additionalProperties.put("modelNamespaceDeclarations",
                             modelPackage.split("\\."));
    additionalProperties.put("modelNamespace",
                             modelPackage.replaceAll("\\.", "::"));
    additionalProperties.put(
        "modelHeaderGuardPrefix",
        modelPackage.replaceAll("\\.", "_").toUpperCase(Locale.ROOT));
    additionalProperties.put("apiNamespaceDeclarations",
                             apiPackage.split("\\."));
    additionalProperties.put("apiNamespace",
                             apiPackage.replaceAll("\\.", "::"));
    additionalProperties.put(
        "apiHeaderGuardPrefix",
        apiPackage.replaceAll("\\.", "_").toUpperCase(Locale.ROOT));
    additionalProperties.put("declspec", declspec);
    additionalProperties.put("defaultInclude", defaultInclude);
    additionalProperties.put(RESERVED_WORD_PREFIX_OPTION, reservedWordPrefix);

    additionalProperties.put("checkparse", new CheckParsing());
    additionalProperties.put("optional", new OptionalLambda());

    additionalProperties.put("camelcase", new CamelCaseLambda());
    additionalProperties.put("uppercaseCamelcase", new CamelCaseLambda(false));
    additionalProperties.put("snakecase", new SnakeCase());
    additionalProperties.put("lowercase", new LowercaseLambda());
    additionalProperties.put("customPath", new CustomPath());
    additionalProperties.put("joinWithCamelCase", new JoinWithCamelCase());
  }

  /**
   * Location to write model files. You can use the modelPackage() as defined
   * when the class is instantiated
   */
  @Override
  public String modelFileFolder() {
    LOGGER.info("Output folder " + outputFolder);
    return outputFolder + "";
  }

  /**
   * Location to write api files. You can use the apiPackage() as defined when
   * the class is instantiated
   */
  @Override
  public String apiFileFolder() {
    return (outputFolder + "/api").replace("/", File.separator);
  }

  private String requestsFileFolder() {
    return (outputFolder + "/" + requestsFolder).replace("/", File.separator);
  }
  private String responsesFileFolder() {
    return (outputFolder + "/" + responsesFolder).replace("/", File.separator);
  }
  private String implFileFolder() {
    return (outputFolder + "/" + implFolder).replace("/", File.separator);
  }
  @Override
  public String apiFilename(String templateName, String tag) {
    String result = super.apiFilename(templateName, tag);

    if (templateName.endsWith("request-header.mustache")) {
      int ix = result.lastIndexOf(File.separatorChar);
      result = result.substring(0, ix) + result.substring(ix, result.length());
      result = result.replace(apiFileFolder(), requestsFileFolder());
    } else if (templateName.endsWith("response-header.mustache")) {
      int ix = result.lastIndexOf(File.separatorChar);
      result = result.substring(0, ix) + result.substring(ix, result.length());
      result = result.replace(apiFileFolder(), responsesFileFolder());
    } else if (templateName.endsWith("handler-header.mustache")) {
      int ix = result.lastIndexOf(File.separatorChar);
      result = result.substring(0, ix) + result.substring(ix, result.length());
      result = result.replace(apiFileFolder(), implFileFolder());
    } else if (templateName.endsWith("handler-source.mustache")) {
      int ix = result.lastIndexOf(File.separatorChar);
      result = result.substring(0, ix) + result.substring(ix, result.length());
      result = result.replace(apiFileFolder(), implFileFolder());
    }
    return result;
  }

  @Override
  public String toModelImport(String name) {
    if (importMapping.containsKey(name)) {
      return importMapping.get(name);
    } else {
      return "#include \"" + toModelFilename(name) + ".hpp\"";
    }
  }

  @Override
  public CodegenModel fromModel(String name, Schema model) {
    CodegenModel codegenModel = super.fromModel(name, model);

    Set<String> oldImports = codegenModel.imports;
    codegenModel.imports = new HashSet<String>();
    for (String imp : oldImports) {
      String newImp = toModelImport(imp);
      if (!newImp.isEmpty()) {
        codegenModel.imports.add(newImp);
      }
    }

    return codegenModel;
  }

  @Override
  public CodegenOperation fromOperation(String path, String httpMethod,
                                        Operation operation,
                                        List<Server> servers) {
    CodegenOperation op =
        super.fromOperation(path, httpMethod, operation, servers);
    if (operation.getResponses() != null &&
        !operation.getResponses().isEmpty()) {
      ApiResponse methodResponse = findMethodResponse(operation.getResponses());

      if (methodResponse != null) {
        Schema response = ModelUtils.getSchemaFromResponse(methodResponse);
        response =
            ModelUtils.unaliasSchema(this.openAPI, response, importMapping);
        if (response != null) {
          CodegenProperty cm = fromProperty("response", response);
          op.vendorExtensions.put("x-codegen-response", cm);
          if ("std::shared_ptr<HttpContent>".equals(cm.dataType)) {
            op.vendorExtensions.put("x-codegen-response-ishttpcontent", true);
          }
        }
      }
    }
    String custom_path = path.replaceAll("\\{(.*?)}", "");
    op.vendorExtensions.put("x-codegen-custom-path", custom_path);
    return op;
  }

  @Override
  public void postProcessModelProperty(CodegenModel model,
                                       CodegenProperty property) {
    if (model.name.endsWith("Config"))
      model.isConfig = true;
    else
      model.isConfig = false;
    LOGGER.info(Boolean.toString(model.isConfig));

    if (isFileSchema(property)) {
      property.vendorExtensions.put("x-codegen-file", true);
    }

    if (!isNullOrEmpty(model.parent)) {
      parentModels.add(model.parent);
      if (!childrenByParent.containsEntry(model.parent, model)) {
        childrenByParent.put(model.parent, model);
      }
    }
  }

  // override with any special post-processing
  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object>
  postProcessOperationsWithModels(Map<String, Object> objs,
                                  List<Object> allModels) {
    Map<String, Object> operations =
        (Map<String, Object>)objs.get("operations");
    List<CodegenOperation> operationList =
        (List<CodegenOperation>)operations.get("operation");
    List<CodegenOperation> newOpList = new ArrayList<CodegenOperation>();

    for (CodegenOperation op : operationList) {
      for (String hdr : op.imports) {
        if (importMapping.containsKey(hdr)) {
          continue;
        }
        operations.put("hasModelImport", true);
        break;
      }
    }

    for (CodegenOperation op : operationList) {
      String path = op.path;

      String[] items = path.split("/", -1);
      String resourceNameCamelCase = "";
      op.path = "";
      Integer counter = 0;

      for (String item : items) {
        counter += 1;
        if (item.length() > 1) {
          if (item.matches("^\\{(.*)\\}$")) {
            String tmpResourceName = item.substring(1, item.length() - 1);
            resourceNameCamelCase +=
                Character.toUpperCase(tmpResourceName.charAt(0)) +
                tmpResourceName.substring(1);
            item = ":" + item.substring(1, item.length() - 1);
          } else {
            resourceNameCamelCase +=
                Character.toUpperCase(item.charAt(0)) + item.substring(1);
          }
        } else if (item.length() == 1) {
          resourceNameCamelCase += Character.toUpperCase(item.charAt(0));
        }
        op.path += item;
        if (counter != items.length)
          op.path += "/";
      }
    }

    // for(String apiName:objs.get("classname"))
    Object all_operations = objs.get("operations");

    String fileModelsNames = ".temp/__env_models_names__.txt";
    String fileApi = ".temp/__env_api__.txt";

    Optional<String> names = null;
    Optional<String> api_check_o = null;

    try {
      names = Files.lines(Paths.get(fileModelsNames)).findFirst();
      api_check_o = Files.lines(Paths.get(fileApi)).findFirst();
    } catch (IOException e) {
      LOGGER.error(
          "Cannot read .temp/__env_api_names__ or .temp/__env_api_paths__ file!(the file for handlers names saving)");
      throw new RuntimeException("Could not open __env___file ", e);
    }

    if (!api_check_o.isPresent())
      return objs;
    String api_check = api_check_o.get();
    if (!api_check.equals("__last__")) {
      supportingFiles.clear();
      return objs;
    }
    try {

      File file = new File("./f");

      if (file.delete()) {
        System.out.println(file.getName() + " is deleted!");
      } else {
        System.out.println("Delete operation is failed.");
      }

    } catch (Exception e) {

      e.printStackTrace();
    }

    String[] modelsNames = null;
    String[] apisTags = null;

    SimpleModel[] modelsList = null;

    try {
      if (names.isPresent()) {
        modelsNames = names.get().split(":");

        List<String> modelsNamesList =
            Arrays.asList(Arrays.copyOf(modelsNames, modelsNames.length));
        Collections.sort(modelsNamesList);
        Set<String> uniqueNamesSet = new HashSet<String>(modelsNamesList);
        List<String> uniqueNamesList = new ArrayList<String>(uniqueNamesSet);
        Collections.sort(uniqueNamesList);

        if (!uniqueNamesList.equals(modelsNamesList)) {

          LOGGER.error("Duplicate models names!");
          throw new RuntimeException("Duplicate models names!");
        }

        modelsList = new SimpleModel[modelsNames.length];
      }
    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    }

    additionalProperties.put("modelsList", modelsList);

    return objs;
  }

  protected boolean isFileSchema(CodegenProperty property) {
    return property.baseType.equals("HttpContent");
  }
  @Override
  public String toModelFilename(String name) {
    return name;
  }

  @Override
  public String toApiFilename(String name) {
    return toApiName(name);
  }

  /**
   * Optional - type declaration. This is a String which is used by the
   * templates to instantiate your types. There is typically special handling
   * for different property types
   *
   * @return a string value used as the `dataType` field for model templates,
   * `returnType` for api templates
   */
  @Override
  public String getTypeDeclaration(Schema p) {
    String openAPIType = getSchemaType(p);

    if (ModelUtils.isArraySchema(p)) {
      ArraySchema ap = (ArraySchema)p;
      Schema inner = ap.getItems();
      return getSchemaType(p) + "<" + getTypeDeclaration(inner) + ">";
    } else if (ModelUtils.isMapSchema(p)) {
      Schema inner = getAdditionalProperties(p);
      return getSchemaType(p) + "<utility::string_t, " +
          getTypeDeclaration(inner) + ">";
    } else if (ModelUtils.isFileSchema(p) || ModelUtils.isBinarySchema(p)) {
      return "std::shared_ptr<" + openAPIType + ">";
    } else if (ModelUtils.isStringSchema(p) || ModelUtils.isDateSchema(p) ||
               ModelUtils.isDateTimeSchema(p) || ModelUtils.isFileSchema(p) ||
               ModelUtils.isUUIDSchema(p) ||
               languageSpecificPrimitives.contains(openAPIType)) {
      return toModelName(openAPIType);
    }

    return "std::shared_ptr<" + openAPIType + ">";
  }

  @Override
  public String toDefaultValue(Schema p) {
    if (ModelUtils.isBooleanSchema(p)) {
      return "false";
    } else if (ModelUtils.isNumberSchema(p)) {
      if (ModelUtils.isFloatSchema(p)) {
        return "0.0f";
      }
      return "0.0";
    } else if (ModelUtils.isIntegerSchema(p)) {
      if (ModelUtils.isLongSchema(p)) {
        return "0L";
      }
      return "0";
    } else if (ModelUtils.isMapSchema(p)) {
      String inner = getSchemaType(getAdditionalProperties(p));
      return "std::map<std::string, " + inner + ">()";
    } else if (ModelUtils.isArraySchema(p)) {
      ArraySchema ap = (ArraySchema)p;
      String inner = getSchemaType(ap.getItems());
      if (!languageSpecificPrimitives.contains(inner) &&
          !inner.contains("string")) {
        inner = "std::shared_ptr<" + inner + ">";
      }
      return "std::vector<" + inner + ">()";
    } else if (!StringUtils.isEmpty(p.get$ref())) {
      return "std::make_shared<" +
          toModelName(ModelUtils.getSimpleRef(p.get$ref())) + ">()";
    } else if (ModelUtils.isStringSchema(p)) {
      return "std::string(\"\")";
    }

    return "nullptr";
  }

  @Override
  public void postProcessParameter(CodegenParameter parameter) {
    super.postProcessParameter(parameter);

    boolean isPrimitiveType = parameter.isPrimitiveType == Boolean.TRUE;
    boolean isListContainer = parameter.isListContainer == Boolean.TRUE;
    boolean isMapContainer = parameter.isMapContainer == Boolean.TRUE;
    boolean isString = parameter.isString == Boolean.TRUE;

    if (!isPrimitiveType && !isListContainer && !isMapContainer && !isString &&
        !parameter.dataType.startsWith("std::shared_ptr")) {
      parameter.dataType = "std::shared_ptr<" + parameter.dataType + ">";
    }
  }

  /**
   * Optional - OpenAPI type conversion. This is used to map OpenAPI types in
   * a `Schema` into either language specific types via `typeMapping` or
   * into complex models if there is not a mapping.
   *
   * @return a string value of the type or complex model for this property
   * @see io.swagger.v3.oas.models.media.Schema
   */
  @Override
  public String getSchemaType(Schema p) {
    String openAPIType = super.getSchemaType(p);
    String type = null;
    if (typeMapping.containsKey(openAPIType)) {
      type = typeMapping.get(openAPIType);
      if (languageSpecificPrimitives.contains(type))
        return toModelName(type);
    } else
      type = openAPIType;
    return toModelName(type);
  }

  @Override
  public Map<String, Object>
  postProcessAllModels(final Map<String, Object> models) {
    final Map<String, Object> processed = super.postProcessAllModels(models);
    postProcessParentModels(models);
    return processed;
  }

  private void postProcessParentModels(final Map<String, Object> models) {

    for (final String parent : parentModels) {
      final CodegenModel parentModel =
          ModelUtils.getModelByName(parent, models);
      final Collection<CodegenModel> childrenModels =
          childrenByParent.get(parent);
      for (final CodegenModel child : childrenModels) {
        processParentPropertiesInChildModel(parentModel, child);
      }
    }
  }

  /**
   * Sets the child property's isInherited flag to true if it is an inherited
   * property
   */
  private void processParentPropertiesInChildModel(final CodegenModel parent,
                                                   final CodegenModel child) {
    final Map<String, CodegenProperty> childPropertiesByName =
        new HashMap<>(child.vars.size());
    if (child != null && child.vars != null && !child.vars.isEmpty()) {
      for (final CodegenProperty childSchema : child.vars) {
        childPropertiesByName.put(childSchema.name, childSchema);
      }
    }

    if (parent != null && parent.vars != null && !parent.vars.isEmpty()) {
      for (final CodegenProperty parentSchema : parent.vars) {
        final CodegenProperty duplicatedByParent =
            childPropertiesByName.get(parentSchema.name);
        if (duplicatedByParent != null) {
          duplicatedByParent.isInherited = true;
        }
      }
    }
  }
}
