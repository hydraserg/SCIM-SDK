package de.gold.scim.schemas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:01 <br>
 * <br>
 * this class can be used to read new resource schemas into the scim context
 */
public final class SchemaFactory
{

  /**
   * the singleton instance of this class
   */
  private static final SchemaFactory INSTANCE = new SchemaFactory();

  /**
   * this map will hold the meta schemata that will define how other schemata must be build
   */
  private final Map<String, Schema> META_SCHEMAS = new HashMap<>();

  /**
   * this map will hold the resource schemata that will define how the resources itself must be build
   */
  private final Map<String, Schema> RESOURCE_SCHEMAS = new HashMap<>();

  /*
   * this block will register the default schemas defined by RFC7643
   */
  private SchemaFactory()
  {
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON));
  }

  /**
   * @return the singleton instance
   */
  public static SchemaFactory getInstance()
  {
    return INSTANCE;
  }

  /**
   * this method is explicitly for unit tests
   */
  static SchemaFactory getUnitTestInstance()
  {
    return new SchemaFactory();
  }

  /**
   * will register a new schema
   *
   * @param jsonSchema the schema as json node
   */
  private void registerMetaSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    META_SCHEMAS.put(schema.getId(), schema);
  }

  /**
   * will register a new resource schema
   *
   * @param jsonSchema the schema as json node
   */
  public void registerResourceSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    List<String> schemas = schema.getSchemas();
    if (schemas.size() != 1)
    {
      String errorMessage = "unexpected number of entries in '" + AttributeNames.SCHEMAS + "' attribute. Expected one"
                            + " entry but was: '" + schemas + "'";
      throw new InvalidSchemaException(errorMessage, null, null, null);
    }

    Supplier<String> message = () -> "meta schema with URI '" + schemas.get(0) + "' is not registered";
    Schema metaSchema = Optional.ofNullable(getMetaSchema(schema.getSchemas().get(0)))
                                .orElseThrow(() -> new InvalidSchemaException(message.get(), null, null, null));

    SchemaValidator.validateDocumentForResponse(metaSchema.toJsonNode(), jsonSchema);
    RESOURCE_SCHEMAS.put(schema.getId(), schema);
  }

  /**
   * extracts a meta schema that will defines the base of another schema like the user resource schema or group
   * resource schema
   *
   * @param id the fully qualified id of the meta schema
   * @return the meta schema if it does exist or null
   */
  public Schema getMetaSchema(String id)
  {
    return META_SCHEMAS.get(id);
  }

  /**
   * extracts a resource schema that will define a resource like "User" or "Group"
   *
   * @param id the fully qualified id of the resource schema
   * @return the resource schema if it does exist or null
   */
  public Schema getResourceSchema(String id)
  {
    return RESOURCE_SCHEMAS.get(id);
  }

}
