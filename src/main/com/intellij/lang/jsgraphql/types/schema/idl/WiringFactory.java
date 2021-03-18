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

import com.intellij.lang.jsgraphql.types.PublicSpi;
import com.intellij.lang.jsgraphql.types.schema.DataFetcher;
import com.intellij.lang.jsgraphql.types.schema.DataFetcherFactory;
import com.intellij.lang.jsgraphql.types.schema.GraphQLScalarType;
import com.intellij.lang.jsgraphql.types.schema.TypeResolver;

import static com.intellij.lang.jsgraphql.types.Assert.assertShouldNeverHappen;

/**
 * A WiringFactory allows you to more dynamically wire in {@link TypeResolver}s and {@link DataFetcher}s
 * based on the IDL definitions.  For example you could look at the directives say to build a more dynamic
 * set of type resolvers and data fetchers.
 */
@PublicSpi
public interface WiringFactory {

    /**
     * This is called to ask if this factory can provide a custom scalar
     *
     * @param environment the wiring environment
     *
     * @return true if the factory can give out a type resolver
     */
    default boolean providesScalar(ScalarWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link GraphQLScalarType} given scalar defined in IDL
     *
     * @param environment the wiring environment
     *
     * @return a {@link GraphQLScalarType}
     */
    default GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }

    /**
     * This is called to ask if this factory can provide a type resolver for the interface
     *
     * @param environment the wiring environment
     *
     * @return true if the factory can give out a type resolver
     */
    default boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link TypeResolver} given the type interface
     *
     * @param environment the wiring environment
     *
     * @return a {@link TypeResolver}
     */
    default TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }

    /**
     * This is called to ask if this factory can provide a type resolver for the union
     *
     * @param environment the wiring environment
     *
     * @return true if the factory can give out a type resolver
     */
    default boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link TypeResolver} given the type union
     *
     * @param environment the union wiring environment
     *
     * @return a {@link TypeResolver}
     */
    default TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }

    /**
     * This is called to ask if this factory can provide a {@link com.intellij.lang.jsgraphql.types.schema.DataFetcherFactory} for the definition
     *
     * @param environment the wiring environment
     *
     * @return true if the factory can give out a data fetcher factory
     */
    default boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link com.intellij.lang.jsgraphql.types.schema.DataFetcherFactory} given the type definition
     *
     * @param environment the wiring environment
     * @param <T>         the type of the data fetcher
     *
     * @return a {@link com.intellij.lang.jsgraphql.types.schema.DataFetcherFactory}
     */
    default <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }

    /**
     * This is called to ask if this factory can provide a schema directive wiring.
     * <p>
     * {@link SchemaDirectiveWiringEnvironment#getDirectives()} contains all the directives
     * available which may in fact be an empty list.
     *
     * @param environment the calling environment
     *
     * @return true if the factory can give out a schema directive wiring.
     */
    default boolean providesSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link com.intellij.lang.jsgraphql.types.schema.idl.SchemaDirectiveWiring} given the environment
     *
     * @param environment the calling environment
     *
     * @return a {@link com.intellij.lang.jsgraphql.types.schema.idl.SchemaDirectiveWiring}
     */
    default SchemaDirectiveWiring getSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }


    /**
     * This is called to ask if this factory can provide a data fetcher for the definition
     *
     * @param environment the wiring environment
     *
     * @return true if the factory can give out a data fetcher
     */
    default boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return false;
    }

    /**
     * Returns a {@link DataFetcher} given the type definition
     *
     * @param environment the wiring environment
     *
     * @return a {@link DataFetcher}
     */
    default DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return assertShouldNeverHappen();
    }

    /**
     * All fields need a data fetcher of some sort and this method is called to provide the data fetcher
     * that will be used if no specific one has been provided
     *
     * @param environment the wiring environment
     *
     * @return a {@link DataFetcher}
     */
    default DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
        return null;
    }
}
