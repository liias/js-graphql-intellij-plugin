/*
    The MIT License (MIT)

    Copyright (c) 2015 Andreas Marek and Contributors

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
    (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
    publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
    OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.intellij.lang.jsgraphql.types.schema.validation;

import com.intellij.lang.jsgraphql.types.schema.*;
import com.intellij.lang.jsgraphql.types.util.FpKit;

import java.util.*;
import java.util.function.Predicate;

import static com.intellij.lang.jsgraphql.types.introspection.Introspection.isIntrospectionTypes;
import static com.intellij.lang.jsgraphql.types.schema.idl.ScalarInfo.isGraphqlSpecifiedScalar;

/**
 * The validation about GraphQLObjectType, GraphQLInterfaceType, GraphQLUnionType, GraphQLEnumType, GraphQLInputObjectType, GraphQLScalarType.
 *     <ul>
 *         <li>Types must define one or more fields;</li>
 *         <li>Enum type must define one or more enum values;</li>
 *         <li>Union type must include one or more unique member types;</li>
 *         <li>The member types of a Union type must all be Object base types;</li>
 *         <li>Non‐Null type must not wrap another Non‐Null type;</li>
 *         <li>Invalid name begin with "__" (two underscores).</li>
 *     </ul>
 * <p>
 * details in https://spec.graphql.org/June2018/#sec-Type-System
 */
public class TypeAndFieldRule implements SchemaValidationRule {

    private Map<String, GraphQLNamedType> schemaTypeHolder;

    @Override
    public void check(GraphQLSchema graphQLSchema, SchemaValidationErrorCollector validationErrorCollector) {

        List<GraphQLNamedType> allTypesAsList = graphQLSchema.getAllTypesAsList();

        List<GraphQLNamedType> filteredType = filterBuiltInTypes(allTypesAsList);

        schemaTypeHolder = graphQLSchema.getTypeMap();

        checkTypes(filteredType, validationErrorCollector);
    }


    private void checkTypes(List<GraphQLNamedType> customizedType, SchemaValidationErrorCollector errorCollector) {
        if (customizedType == null || customizedType.isEmpty()) {
            return;
        }

        for (GraphQLType type : customizedType) {
            checkType(type, errorCollector);
        }
    }

    private void checkType(GraphQLType type, SchemaValidationErrorCollector errorCollector) {
        if (type instanceof GraphQLObjectType || type instanceof GraphQLInterfaceType) {
            validateContainsField((GraphQLFieldsContainer) type, errorCollector);
        } else if (type instanceof GraphQLUnionType) {
            validateUnion((GraphQLUnionType) type, errorCollector);
        } else if (type instanceof GraphQLEnumType) {
            validateEnum((GraphQLEnumType) type, errorCollector);
        } else if (type instanceof GraphQLInputObjectType) {
            validateInputObject((GraphQLInputObjectType) type, errorCollector);
        } else if (type instanceof GraphQLScalarType) {
            validateScalar((GraphQLScalarType) type, errorCollector);
        }
    }

    private void validateContainsField(GraphQLFieldsContainer type, SchemaValidationErrorCollector errorCollector) {
        assertTypeName(type.getName(), errorCollector);

        List<GraphQLFieldDefinition> fieldDefinitions = type.getFieldDefinitions();
        if (fieldDefinitions == null || fieldDefinitions.size() == 0) {
            SchemaValidationError validationError = new SchemaValidationError(SchemaValidationErrorType.ImplementingTypeLackOfFieldError, String.format("\"%s\" must define one or more fields.", type.getName()));
            errorCollector.addError(validationError);
            return;
        }

        for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
            validateFieldDefinition(type.getName(), fieldDefinition, errorCollector);
        }
    }

    private void validateInputObject(GraphQLInputObjectType type, SchemaValidationErrorCollector errorCollector) {
        assertTypeName(type.getName(), errorCollector);

        List<GraphQLInputObjectField> inputObjectFields = type.getFields();
        if (inputObjectFields == null || inputObjectFields.size() == 0) {
            SchemaValidationError validationError = new SchemaValidationError(SchemaValidationErrorType.InputObjectTypeLackOfFieldError, String.format("\"%s\" must define one or more fields.", type.getName()));
            errorCollector.addError(validationError);
            return;
        }

        for (GraphQLInputObjectField inputObjectField : inputObjectFields) {
            validateInputFieldDefinition(inputObjectField.getName(), inputObjectField, errorCollector);
        }
    }

    private void validateUnion(GraphQLUnionType type, SchemaValidationErrorCollector errorCollector) {
        assertTypeName(type.getName(), errorCollector);

        List<GraphQLNamedOutputType> memberTypes = type.getTypes();
        if (memberTypes == null || memberTypes.size() == 0) {
            SchemaValidationError validationError =
                    new SchemaValidationError(SchemaValidationErrorType.UnionTypeLackOfTypeError, String.format("Union type \"%s\" must include one or more unique member types.", type.getName()));
            errorCollector.addError(validationError);
        }

        Set<String> typeNames = new HashSet<>();
        for (GraphQLNamedOutputType memberType : memberTypes) {
            String typeName = memberType.getName();
            GraphQLNamedType graphQLNamedType = schemaTypeHolder.get(typeName);
            if (!(graphQLNamedType instanceof GraphQLObjectType)) {
                SchemaValidationError validationError =
                        new SchemaValidationError(SchemaValidationErrorType.InvalidUnionMemberTypeError, String.format("The member types of a Union type must all be Object base types. member type \"%s\" in Union \"%s\" is invalid.", memberType.getName(), type.getName()));
                errorCollector.addError(validationError);
            }
            if (typeNames.contains(typeName)) {
                SchemaValidationError validationError =
                        new SchemaValidationError(SchemaValidationErrorType.RepetitiveElementError, String.format("The member types of a Union type must be unique. member type \"%s\" in Union \"%s\" is not unique.", memberType.getName(), type.getName()));
                errorCollector.addError(validationError);
            }
            typeNames.add(typeName);
        }
    }

    private void validateScalar(GraphQLScalarType type, SchemaValidationErrorCollector errorCollector) {
        assertTypeName(type.getName(), errorCollector);
    }

    private void validateEnum(GraphQLEnumType type, SchemaValidationErrorCollector errorCollector) {
        assertTypeName(type.getName(), errorCollector);

        List<GraphQLEnumValueDefinition> enumValueDefinitions = type.getValues();
        if (enumValueDefinitions == null || enumValueDefinitions.size() == 0) {
            SchemaValidationError validationError = new SchemaValidationError(SchemaValidationErrorType.EnumLackOfValueError, String.format("Enum type \"%s\" must define one or more enum values.", type.getName()));
            errorCollector.addError(validationError);
        } else {
            for (GraphQLEnumValueDefinition enumValueDefinition : enumValueDefinitions) {
                assertEnumValueDefinitionName(type.getName(), enumValueDefinition.getName(), errorCollector);
            }
        }

    }

    private void validateFieldDefinition(String typeName, GraphQLFieldDefinition fieldDefinition, SchemaValidationErrorCollector errorCollector) {
        assertFieldName(typeName, fieldDefinition.getName(), errorCollector);
        assertNonNullType(fieldDefinition.getType(), errorCollector);

        List<GraphQLArgument> fieldDefinitionArguments = fieldDefinition.getArguments();
        if (fieldDefinitionArguments != null || fieldDefinitionArguments.size() != 0) {
            for (GraphQLArgument fieldDefinitionArgument : fieldDefinitionArguments) {
                validateFieldDefinitionArgument(typeName, fieldDefinition.getName(), fieldDefinitionArgument, errorCollector);
            }
        }
    }

    private void validateInputFieldDefinition(String typeName, GraphQLInputObjectField inputObjectField, SchemaValidationErrorCollector errorCollector) {
        assertFieldName(typeName, inputObjectField.getName(), errorCollector);
        assertNonNullType(inputObjectField.getType(), errorCollector);
    }

    private void validateFieldDefinitionArgument(String typeName, String fieldName, GraphQLArgument argument, SchemaValidationErrorCollector errorCollector) {
        assertArgumentName(typeName, fieldName, argument.getName(), errorCollector);
        assertNonNullType(argument.getType(), errorCollector);
    }

    private void assertTypeName(String typeName, SchemaValidationErrorCollector validationErrorCollector) {
        if (typeName.length() >= 2 && typeName.startsWith("__")) {
            SchemaValidationError schemaValidationError = new SchemaValidationError(SchemaValidationErrorType.InvalidCustomizedNameError,
                    String.format("\"%s\" must not begin with \"__\", which is reserved by GraphQL introspection.", typeName));
            validationErrorCollector.addError(schemaValidationError);
        }
    }

    private void assertFieldName(String typeName, String fieldName, SchemaValidationErrorCollector errorCollector) {
        if (fieldName.length() >= 2 && fieldName.startsWith("__")) {
            SchemaValidationError schemaValidationError = new SchemaValidationError(SchemaValidationErrorType.InvalidCustomizedNameError,
                    String.format("\"%s\" in \"%s\" must not begin with \"__\", which is reserved by GraphQL introspection.", fieldName, typeName));
            errorCollector.addError(schemaValidationError);
        }
    }

    private void assertArgumentName(String typeName, String fieldName, String argumentName, SchemaValidationErrorCollector errorCollector) {
        if (argumentName.length() >= 2 && argumentName.startsWith("__")) {
            SchemaValidationError schemaValidationError = new SchemaValidationError(SchemaValidationErrorType.InvalidCustomizedNameError,
                    String.format("Argument name \"%s\" in \"%s-%s\" must not begin with \"__\", which is reserved by GraphQL introspection.", argumentName, typeName, fieldName));
            errorCollector.addError(schemaValidationError);
        }
    }

    private void assertEnumValueDefinitionName(String typeName, String enumValueDefinitionName, SchemaValidationErrorCollector errorCollector) {
        if (enumValueDefinitionName.length() >= 2 && enumValueDefinitionName.startsWith("__")) {
            SchemaValidationError schemaValidationError = new SchemaValidationError(SchemaValidationErrorType.InvalidCustomizedNameError,
                    String.format("EnumValueDefinition \"%s\" in  \"%s\"  must not begin with \"__\", which is reserved by GraphQL introspection.", enumValueDefinitionName, typeName));
            errorCollector.addError(schemaValidationError);
        }
    }

    private void assertNonNullType(GraphQLType type, SchemaValidationErrorCollector errorCollector) {
        if (type instanceof GraphQLNonNull && ((GraphQLNonNull) type).getWrappedType() instanceof GraphQLNonNull) {
            SchemaValidationError schemaValidationError = new SchemaValidationError(SchemaValidationErrorType.NonNullWrapNonNullError,
                    String.format("Non‐Null type must not wrap another Non‐Null type: \"%s\" is invalid.", GraphQLTypeUtil.simplePrint(type)));
            errorCollector.addError(schemaValidationError);
        }
    }

    private List<GraphQLNamedType> filterBuiltInTypes(List<GraphQLNamedType> graphQLNamedTypes) {
        if (graphQLNamedTypes == null || graphQLNamedTypes.isEmpty()) {
            return Collections.emptyList();
        }

        Predicate<GraphQLNamedType> filterFunction = namedType -> {
            if (isIntrospectionTypes(namedType)) {
                return false;
            }
            if (namedType instanceof GraphQLScalarType && isGraphqlSpecifiedScalar((GraphQLScalarType) namedType)) {
                return false;
            }
            return true;
        };

        return FpKit.filterList(graphQLNamedTypes, filterFunction);
    }


    @Override
    public void check(GraphQLFieldDefinition fieldDef, SchemaValidationErrorCollector validationErrorCollector) {
    }

    @Override
    public void check(GraphQLType type, SchemaValidationErrorCollector validationErrorCollector) {
    }
}
