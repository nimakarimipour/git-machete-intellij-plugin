package com.virtuslab.gitmachete.frontend.actions.common;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.Collections;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BasePushBranchAction extends DumbAwareAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  protected final List<SyncToRemoteStatus.Relation> PUSH_ELIGIBLE_STATUSES = List.of(
      SyncToRemoteStatus.Relation.Ahead,
      SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote,
      SyncToRemoteStatus.Relation.DivergedAndOlderThanRemote,
      SyncToRemoteStatus.Relation.Untracked);

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data datakeys in those {@code update} implementations will still do be available
   * in {@link BasePushBranchAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  @UIEffect
  protected void doPush(Project project, GitRepository preselectedRepository, String branchName) {
    @Nullable
    GitLocalBranch localBranch = preselectedRepository.getBranches().findLocalBranch(branchName);

    if (localBranch != null) {
      java.util.List<GitRepository> selectedRepositories = Collections.singletonList(preselectedRepository);
      // Presented dialog shows commits for branches belonging to allRepositories, preselectedRepositories and currentRepo.
      // The second and the third one have higher priority of loading its commits.
      // From our perspective, we always have single (pre-selected) repository so we do not care about the priority.
      new VcsPushDialog(project,
          /* allRepositories */ selectedRepositories,
          /* preselectedRepositories */ selectedRepositories,
          /* currentRepo */ null,
          GitPushSource.create(localBranch)).show();
    } else {
      LOG.warn("Skipping the action because provided branch ${branchName} was not found in repository");
    }
  }

  protected String getRelationBasedDescription(SyncToRemoteStatus.Relation relation) {
    String descriptionSpec = Match(relation).of(
        Case($(SyncToRemoteStatus.Relation.Behind), "behind its remote"),
        Case($(SyncToRemoteStatus.Relation.InSync), "in sync to its remote"),
        Case($(), "in unknown status '${relation.toString()}' to its remote"));
    return "Push disabled because current branch is ${descriptionSpec}";
  }
}