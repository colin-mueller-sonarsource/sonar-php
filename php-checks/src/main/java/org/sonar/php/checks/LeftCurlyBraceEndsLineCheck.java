/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.php.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.php.api.PHPPunctuator;
import org.sonar.php.lexer.PHPTagsChannel;
import org.sonar.php.parser.PHPGrammar;

@Rule(
  key = "S1105",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class LeftCurlyBraceEndsLineCheck extends SquidCheck<Grammar> {

  @Override
  public void init() {
    subscribeTo(PHPPunctuator.LCURLYBRACE);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (!isExcluded(astNode) && (isFirstOnLine(astNode) || !isLastOnLine(astNode))) {
      getContext().createLineViolation(this, "Move this open curly brace to the end of the previous line.", astNode);
    }
  }

  private static boolean isLastOnLine(AstNode lcurly) {
    return getNextNodeTokenLine(lcurly.getNextAstNode()) != lcurly.getTokenLine();
  }

  private static boolean isFirstOnLine(AstNode lcurly) {
    return lcurly.getPreviousAstNode().getLastToken().getLine() != lcurly.getTokenLine();
  }

  private static boolean isExcluded(AstNode lcurly) {
    return isOnSameLineThanRightCurlyBrace(lcurly) || isStatementBlock(lcurly);
  }

  private static boolean isStatementBlock(AstNode lcurly) {
    return lcurly.getParent().getParent().is(PHPGrammar.STATEMENT)
      && lcurly.getParent().getParent().getParent().isNot(
      PHPGrammar.DECLARE_STATEMENT,
      PHPGrammar.FOREACH_STATEMENT,
      PHPGrammar.FOR_STATEMENT,
      PHPGrammar.DO_WHILE_STATEMENT,
      PHPGrammar.WHILE_STATEMENT,
      PHPGrammar.ELSE_CLAUSE,
      PHPGrammar.ELSEIF_CLAUSE,
      PHPGrammar.IF_STATEMENT);
  }

  private static boolean isOnSameLineThanRightCurlyBrace(AstNode lcurly) {
    return lcurly.getParent().getFirstChild(PHPPunctuator.RCURLYBRACE).getTokenLine() == lcurly.getTokenLine();
  }

  /**
   * Returns line of next token's node skipping INLINE_HTML node.
   */
  private static int getNextNodeTokenLine(AstNode lcurlyNextAstNode) {
    int nextTokenLine = lcurlyNextAstNode.getTokenLine();

    if (lcurlyNextAstNode.is(PHPGrammar.INNER_STATEMENT_LIST)) {
      AstNode firstStatement = lcurlyNextAstNode.getFirstChild();
      if (firstStatement.is(PHPGrammar.STATEMENT) && firstStatement.getFirstChild().is(PHPTagsChannel.INLINE_HTML)) {
        nextTokenLine = firstStatement.getNextAstNode().getTokenLine();
      }
    }
    return nextTokenLine;
  }
}