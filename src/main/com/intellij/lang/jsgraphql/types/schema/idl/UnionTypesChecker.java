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
package com.intellij.lang.jsgraphql.types.schema.idl;

import com.intellij.lang.jsgraphql.types.GraphQLError;
import com.intellij.lang.jsgraphql.types.Internal;
import com.intellij.lang.jsgraphql.types.language.*;
import com.intellij.lang.jsgraphql.types.schema.idl.errors.UnionTypeError;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * UnionType check, details in https://spec.graphql.org/June2018/#sec-Type-System.
 * <pre>
 *     <ur>
 *         <li>Invalid name begin with "__" (two underscores);</li>
 *         <li>Union type must include one or more member types;</li>
 *         <li>The member types of a Union type must all be Object base types;</li>
 *         <li>The member types of a Union type must be unique.</li>
 *     </ur>
 * </pre>
 */
@Internal
class UnionTypesChecker {
    private static final Map<Class<? extends UnionTypeDefinition>, String> TYPE_OF_MAP = new HashMap<>();

    static {
        TYPE_OF_MAP.put(UnionTypeDefinition.class, "union");
        TYPE_OF_MAP.put(UnionTypeExtensionDefinition.class, "union extension");
    }


    void checkUnionType(List<GraphQLError> errors, TypeDefinitionRegistry typeRegistry) {
        List<UnionTypeDefinition> unionTypes = typeRegistry.getTypes(UnionTypeDefinition.class);
        List<UnionTypeExtensionDefinition> unionTypeExtensions = typeRegistry.getTypes(UnionTypeExtensionDefinition.class);

        //noinspection RedundantCast
        Stream.of(unionTypes.stream(), unionTypeExtensions.stream())
                .flatMap(Function.identity())
                .forEach(type -> checkUnionType(typeRegistry, ((UnionTypeDefinition) type), errors));
    }

    private void checkUnionType(TypeDefinitionRegistry typeRegistry, UnionTypeDefinition unionTypeDefinition, List<GraphQLError> errors) {
        assertTypeName(unionTypeDefinition, errors);

        List<Type> memberTypes = unionTypeDefinition.getMemberTypes();
        if (memberTypes == null || memberTypes.size() == 0) {
            errors.add(new UnionTypeError(unionTypeDefinition, format("Union type '%s' must include one or more member types.", unionTypeDefinition.getName())));
            return;
        }

        Set<String> typeNames = new LinkedHashSet<>();
        for (Type memberType : memberTypes) {
            String memberTypeName = ((TypeName) memberType).getName();
            Optional<TypeDefinition> memberTypeDefinition = typeRegistry.getType(memberTypeName);

            if (!memberTypeDefinition.isPresent() || !(memberTypeDefinition.get() instanceof ObjectTypeDefinition)) {
                errors.add(new UnionTypeError(unionTypeDefinition, format("The member types of a Union type must all be Object base types. member type '%s' in Union '%s' is invalid.", ((TypeName) memberType).getName(), unionTypeDefinition.getName())));
                continue;
            }

            if (typeNames.contains(memberTypeName)) {
                errors.add(new UnionTypeError(unionTypeDefinition, format("member type '%s' in Union '%s' is not unique. The member types of a Union type must be unique.", ((TypeName) memberType).getName(), unionTypeDefinition.getName())));
                continue;
            }
            typeNames.add(memberTypeName);
        }
    }

    private void assertTypeName(UnionTypeDefinition unionTypeDefinition, List<GraphQLError> errors) {
        if (unionTypeDefinition.getName().length() >= 2 && unionTypeDefinition.getName().startsWith("__")) {
            errors.add((new UnionTypeError(unionTypeDefinition, String.format("'%s' must not begin with '__', which is reserved by GraphQL introspection.", unionTypeDefinition.getName()))));
        }
    }

}
