/*
 * Copyright (c) 2018-present, Jim Kynde Meyer
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.intellij.lang.jsgraphql.schema

import com.google.common.collect.Lists
import com.intellij.lang.jsgraphql.ide.search.GraphQLScopeProvider
import com.intellij.lang.jsgraphql.types.GraphQLException
import com.intellij.lang.jsgraphql.types.schema.GraphQLObjectType
import com.intellij.lang.jsgraphql.types.schema.GraphQLSchema
import com.intellij.lang.jsgraphql.types.schema.idl.UnExecutableSchemaGenerator
import com.intellij.lang.jsgraphql.types.schema.validation.InvalidSchemaException
import com.intellij.lang.jsgraphql.types.schema.validation.SchemaValidator
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.TimeoutUtil
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.ConcurrentMap

@Service
class GraphQLSchemaProvider(project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project) = project.service<GraphQLSchemaProvider>()

        private val LOG = logger<GraphQLSchemaProvider>()

        private val EMPTY_SCHEMA = GraphQLSchema.newSchema()
            .query(GraphQLObjectType.newObject().name("Query").build()).build()
    }

    private val registryProvider = GraphQLRegistryProvider.getInstance(project)
    private val scopeProvider = GraphQLScopeProvider.getInstance(project)

    // TODO: check if soft reference is not collected too early
    private val scopeToSchemaCache: CachedValue<ConcurrentMap<GlobalSearchScope, GraphQLSchemaInfo>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(
                ContainerUtil.createConcurrentSoftMap(),
                GraphQLSchemaUtil.getSchemaDependencies(project),
            )
        }

    fun getSchemaInfo(context: PsiElement?): GraphQLSchemaInfo {
        val scope = scopeProvider.getResolveScope(context)

        return scopeToSchemaCache.value.computeIfAbsent(scope) {
            val registryWithErrors = registryProvider.getRegistryInfo(context)

            try {
                val start = System.nanoTime()
                val schema =
                    UnExecutableSchemaGenerator.makeUnExecutableSchema(registryWithErrors.typeDefinitionRegistry)
                val validationErrors = SchemaValidator().validateSchema(schema)
                val errors = if (validationErrors.isEmpty())
                    emptyList()
                else
                    listOf<GraphQLException>(InvalidSchemaException(validationErrors))

                LOG.debug {
                    String.format(
                        "Schema build completed in %d ms, requester: %s",
                        TimeoutUtil.getDurationMillis(start),
                        context?.containingFile?.name ?: scope
                    )
                }
                GraphQLSchemaInfo(schema, errors, registryWithErrors)
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                LOG.error("Schema build error: ", e) // should never happen

                GraphQLSchemaInfo(
                    EMPTY_SCHEMA,
                    Lists.newArrayList(if (e is GraphQLException) e else GraphQLException(e)),
                    registryWithErrors
                )
            }
        }
    }
}
