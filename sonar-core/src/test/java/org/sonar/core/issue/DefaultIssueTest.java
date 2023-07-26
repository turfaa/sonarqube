/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.utils.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIssueTest {

  private final DefaultIssue issue = new DefaultIssue();

  @Test
  public void set_empty_dates() {
    issue
      .setCreationDate(null)
      .setUpdateDate(null)
      .setCloseDate(null)
      .setSelectedAt(null);

    assertThat(issue.creationDate()).isNull();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.selectedAt()).isNull();
  }

  @Test
  public void fail_on_empty_status() {
    try {
      issue.setStatus("");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Status must be set");
    }
  }

  @Test
  public void fail_on_bad_severity() {
    try {
      issue.setSeverity("FOO");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid severity: FOO");
    }
  }

  @Test
  public void message_should_be_abbreviated_if_too_long() {
    issue.setMessage(StringUtils.repeat("a", 5_000));
    assertThat(issue.message()).hasSize(1_333);
  }

  @Test
  public void message_could_be_null() {
    issue.setMessage(null);
    assertThat(issue.message()).isNull();
  }

  @Test
  public void test_nullable_fields() {
    issue.setGap(null).setSeverity(null).setLine(null);
    assertThat(issue.gap()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.line()).isNull();
  }

  @Test
  public void test_equals_and_hashCode() {
    DefaultIssue a1 = new DefaultIssue().setKey("AAA");
    DefaultIssue a2 = new DefaultIssue().setKey("AAA");
    DefaultIssue b = new DefaultIssue().setKey("BBB");
    assertThat(a1)
      .isEqualTo(a1)
      .isEqualTo(a2)
      .isNotEqualTo(b)
      .hasSameHashCodeAs(a1);
  }

  @Test
  public void comments_should_not_be_modifiable() {
    DefaultIssue issue = new DefaultIssue().setKey("AAA");

    List<DefaultIssueComment> comments = issue.defaultIssueComments();
    assertThat(comments).isEmpty();
    DefaultIssueComment defaultIssueComment = new DefaultIssueComment();
    try {
      comments.add(defaultIssueComment);
      fail();
    } catch (UnsupportedOperationException e) {
      // ok
    } catch (Exception e) {
      fail("Unexpected exception: " + e);
    }
  }

  @Test
  public void all_changes_contain_current_change() {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    when(issueChangeContext.getExternalUser()).thenReturn("toto");
    when(issueChangeContext.getWebhookSource()).thenReturn("github");

    DefaultIssue issue = new DefaultIssue()
      .setKey("AAA")
      .setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");

    assertThat(issue.changes()).hasSize(1);
    FieldDiffs actualDiffs = issue.changes().iterator().next();
    assertThat(actualDiffs.externalUser()).contains(issueChangeContext.getExternalUser());
    assertThat(actualDiffs.webhookSource()).contains(issueChangeContext.getWebhookSource());
  }

  @Test
  public void setFieldChange_whenAddingChange_shouldUpdateCurrentChange() {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    DefaultIssue issue = new DefaultIssue().setKey("AAA");

    issue.setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");
    assertThat(issue.changes()).hasSize(1);
    FieldDiffs currentChange = issue.currentChange();
    assertThat(currentChange).isNotNull();
    assertThat(currentChange.get("actionPlan")).isNotNull();
    assertThat(currentChange.get("authorLogin")).isNull();

    issue.setFieldChange(issueChangeContext, "authorLogin", null, "testuser");
    assertThat(issue.changes()).hasSize(1);
    assertThat(currentChange.get("actionPlan")).isNotNull();
    assertThat(currentChange.get("authorLogin")).isNotNull();
    assertThat(currentChange.get("authorLogin").newValue()).isEqualTo("testuser");
  }

  @Test
  public void adding_null_change_has_no_effect() {
    DefaultIssue issue = new DefaultIssue();

    issue.addChange(null);

    assertThat(issue.changes()).isEmpty();
  }

  @Test
  public void test_isToBeMigratedAsNewCodeReferenceIssue_is_correctly_calculated() {
    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isTrue();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();
  }

  @Test
  public void isQuickFixAvailable_givenQuickFixAvailable_returnTrue() {
    DefaultIssue defaultIssue = new DefaultIssue();

    defaultIssue.setQuickFixAvailable(true);

    assertThat(defaultIssue.isQuickFixAvailable()).isTrue();

    defaultIssue.setQuickFixAvailable(false);

    assertThat(defaultIssue.isQuickFixAvailable()).isFalse();
  }

  @Test
  public void characteristic_shouldReturnNull() {
    DefaultIssue defaultIssue = new DefaultIssue();
    assertThat(defaultIssue.characteristic()).isNull();
  }

  @Test
  public void setLine_whenLineIsNegative_shouldThrowException() {
    int anyNegativeValue = Integer.MIN_VALUE;
    assertThatThrownBy(() -> issue.setLine(anyNegativeValue))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Line must be null or greater than zero (got %s)", anyNegativeValue));
  }

  @Test
  public void setLine_whenLineIsZero_shouldThrowException() {
    assertThatThrownBy(() -> issue.setLine(0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Line must be null or greater than zero (got 0)");
  }

  @Test
  public void setGap_whenGapIsNegative_shouldThrowException() {
    Double anyNegativeValue = -1.0;
    assertThatThrownBy(() -> issue.setGap(anyNegativeValue))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Gap must be greater than or equal 0 (got %s)", anyNegativeValue));
  }

  @Test
  public void setGap_whenGapIsZero_shouldWork() {
    issue.setGap(0.0);
    assertThat(issue.gap()).isEqualTo(0.0);
  }

  @Test
  public void effortInMinutes_shouldConvertEffortToMinutes() {
    issue.setEffort(Duration.create(60));
    assertThat(issue.effortInMinutes()).isEqualTo(60L);
  }

  @Test
  public void effortInMinutes_whenNull_shouldReturnNull() {
    issue.setEffort(null);
    assertThat(issue.effortInMinutes()).isNull();
  }

  @Test
  public void tags_whenNull_shouldReturnEmptySet() {
    assertThat(issue.tags()).isEmpty();
  }

  @Test
  public void codeVariants_whenNull_shouldReturnEmptySet() {
    assertThat(issue.codeVariants()).isEmpty();
  }

  @Test
  public void issueByDefault_shouldNotHaveAppliedAnticipatedTransitions() {
    DefaultIssue defaultIssue = new DefaultIssue();
    assertThat(defaultIssue.getAnticipatedTransitionUuid()).isNotPresent();
  }

  @Test
  public void anticipatedTransitions_WhenSetTrue_shouldReturnTrue() {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setAnticipatedTransitionUuid("uuid");
    assertThat(defaultIssue.getAnticipatedTransitionUuid()).isPresent();
  }
}
