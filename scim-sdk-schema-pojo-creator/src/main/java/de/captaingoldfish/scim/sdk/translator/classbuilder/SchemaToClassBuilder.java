package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 03.05.2021
 */
public class SchemaToClassBuilder
{

  public String generateClassFromSchema(Schema schema)
  {
    String packageName = "package ???";
    String imports = String.format("import %s", getImports());
    String javadoc = schema.getDescription().orElse("");
    String className = StringUtils.capitalize(schema.getName().orElse("Unknown"));

    String classStructure = buildClassStructure(schema);
    return String.format("%s\n\n%s\n\n/** %s */\npublic class %s extends ResourceNode\n{\n  %s\n}",
                         packageName,
                         imports,
                         javadoc,
                         className,
                         classStructure);
  }

  private String getImports()
  {
    // @formatter:off
    return "import java.util.Arrays;\n" +
            "import java.util.List;\n" +
            "import java.util.Optional;\n" +
            "import java.util.Set;\n" +
            "import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;\n" +
            "import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;";
    // @formatter:on
  }

  private String buildClassStructure(Schema schema)
  {
    List<String> constructorAttributes = new ArrayList<>();
    List<String> setterMethodCalls = new ArrayList<>();
    List<String> getterAndSetterMethodDefinitions = new ArrayList<>();
    List<String> complexAttributeDefinitions = new ArrayList<>();

    for ( SchemaAttribute attribute : schema.getAttributes() )
    {
      if (Type.COMPLEX.equals(attribute.getType()))
      {
        final ComplexAttributeToInnerClassBuilder complexBuilder = new ComplexAttributeToInnerClassBuilder();
        String innerClassDefinition = complexBuilder.generateComplexAttributeClass(attribute);
        complexAttributeDefinitions.add(innerClassDefinition);
        setterMethodCalls.add(complexBuilder.getSetterMethodCall());
        constructorAttributes.add(complexBuilder.getSetterParameter());
      }
      else
      {
        final GetterMethodBuilder getterMethodBuilder = new GetterMethodBuilder();
        final SetterMethodBuilder setterMethodBuilder = new SetterMethodBuilder();
        String getterMethod = getterMethodBuilder.generateSimpleGetterMethod(attribute);
        String setterMethod = setterMethodBuilder.generateSimpleSetterMethod(attribute);

        getterAndSetterMethodDefinitions.add(String.format("%s\n%s", getterMethod, setterMethod));
        setterMethodCalls.add(setterMethodBuilder.getSetterCall());
        constructorAttributes.add(setterMethodBuilder.getSetterParameter());
      }
    }

    String constructor = getConstructor(schema, constructorAttributes, setterMethodCalls);
    StringBuilder getterAndSetterMethodCalls = new StringBuilder();
    for ( int i = 0 ; i < getterAndSetterMethodDefinitions.size() ; i++ )
    {
      String getterAndSetterDefinition = getterAndSetterMethodDefinitions.get(i);
      getterAndSetterMethodCalls.append(getterAndSetterDefinition).append('\n');
    }

    return String.format("%s%s", constructor, getterAndSetterMethodCalls);
  }

  private String getConstructor(Schema schema, List<String> constructorAttributes, List<String> setterMethodCalls)
  {
    String constructorParams = String.join(", ", constructorAttributes);
    String setterCalls = String.join("\n  ", setterMethodCalls);
    String constructorName = StringUtils.capitalize(schema.getName().orElse(null));
    return String.format("public %s(%s) \n  {\n  %s\n  }\n", constructorName, constructorParams, setterCalls);
  }
}
