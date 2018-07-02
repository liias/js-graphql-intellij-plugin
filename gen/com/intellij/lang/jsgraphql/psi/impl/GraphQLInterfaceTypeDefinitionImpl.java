// This is a generated file. Not intended for manual editing.
package com.intellij.lang.jsgraphql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.jsgraphql.psi.GraphQLElementTypes.*;
import com.intellij.lang.jsgraphql.psi.*;

public class GraphQLInterfaceTypeDefinitionImpl extends GraphQLTypeDefinitionImpl implements GraphQLInterfaceTypeDefinition {

  public GraphQLInterfaceTypeDefinitionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull GraphQLVisitor visitor) {
    visitor.visitInterfaceTypeDefinition(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GraphQLVisitor) accept((GraphQLVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public GraphQLFieldsDefinition getFieldsDefinition() {
    return findChildByClass(GraphQLFieldsDefinition.class);
  }

  @Override
  @Nullable
  public GraphQLTypeNameDefinition getTypeNameDefinition() {
    return findChildByClass(GraphQLTypeNameDefinition.class);
  }

  @Override
  @Nullable
  public GraphQLQuotedString getDescription() {
    return findChildByClass(GraphQLQuotedString.class);
  }

  @Override
  @NotNull
  public List<GraphQLDirective> getDirectives() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, GraphQLDirective.class);
  }

}