package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.List;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseRebaseBranchOntoParentAction extends GitMacheteRepositoryReadyAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    Option<Repository.State> state = getSelectedVcsRepository(anActionEvent).map(r -> r.getState());

    if (state.isEmpty()) {
      presentation.setEnabled(false);
    } else if (state.get() != Repository.State.NORMAL) {
      // `REVERTING` state is available since 193.2495, but we're still supporting 192.*
      var revertingState = Try.of(() -> Repository.State.valueOf("REVERTING")).getOrNull();

      var stateName = Match(state.get()).of(
          // In versions earlier than 193.2495 if repository is in reverting state,
          // com.intellij.dvcs.repo.Repository.getState returns `GRAFTING` state like when cherry-pick is in progress so
          // we return custom message in this case
          Case($(Repository.State.GRAFTING),
              revertingState != null ? "during an ongoing cherry-pick" : "during an ongoing cherry-pick or revert"),
          Case($(Repository.State.DETACHED), "in the detached head state"),
          Case($(Repository.State.MERGING), "during an ongoing merge"),
          Case($(Repository.State.REBASING), "during an ongoing rebase"),
          Case($(revertingState), "during an ongoing revert"),
          Case($(), ": " + state.toString().toLowerCase()));

      presentation.setEnabled(false);
      presentation.setDescription("Can't rebase ${stateName}");
    }
  }

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data datakeys in those {@code update} implementations will still do be available
   * in {@link BaseRebaseBranchOntoParentAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  protected void doRebase(AnActionEvent anActionEvent, BaseGitMacheteNonRootBranch branchToRebase) {
    Project project = ActionUtils.getProject(anActionEvent);
    Option<GitRepository> gitRepository = getSelectedVcsRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      doRebase(project, gitRepository.get(), branchToRebase);
    } else {
      LOG.warn("Skipping the action because Git repository is undefined");
    }
  }

  private void doRebase(Project project, GitRepository gitRepository, BaseGitMacheteNonRootBranch branchToRebase) {
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    Try.of(() -> branchToRebase.getParametersForRebaseOntoParent())
        .onSuccess(gitRebaseParameters -> {
          LOG.debug(() -> "Queuing '${branchToRebase.getName()}' branch rebase background task");

          new Task.Backgroundable(project, "Rebasing") {
            @Override
            public void run(ProgressIndicator indicator) {
              GitRebaseParams params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
              LOG.info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
                  "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
                  "onto ${gitRebaseParameters.getNewBaseCommit().getHash()}");

              GitRebaseUtils.rebase(project, List.of(gitRepository), params, indicator);
            }

            // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
            // changes to commits (eg. commits may get squashed so the graph structure changes).
          }.queue();
        }).onFailure(e -> {
          // TODO (#172): redirect the user to the manual fork-point
          var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
          LOG.error(message);
          VcsNotifier.getInstance(project).notifyError("Rebase failed", message);
          GuiUtils.invokeLaterIfNeeded(() -> Messages.showErrorDialog(message, "Something Went Wrong..."),
              ModalityState.NON_MODAL);
        });
  }

  private GitRebaseParams getIdeaRebaseParamsOf(GitRepository repository, IGitRebaseParameters gitRebaseParameters) {
    GitVersion gitVersion = repository.getVcs().getVersion();
    String currentBranch = gitRebaseParameters.getCurrentBranch().getName();
    String newBase = gitRebaseParameters.getNewBaseCommit().getHash();
    String forkPoint = gitRebaseParameters.getForkPointCommit().getHash();

    return new GitRebaseParams(gitVersion, currentBranch, newBase, /* upstream */ forkPoint,
        /* interactive */ true, /* preserveMerges */ false);
  }
}