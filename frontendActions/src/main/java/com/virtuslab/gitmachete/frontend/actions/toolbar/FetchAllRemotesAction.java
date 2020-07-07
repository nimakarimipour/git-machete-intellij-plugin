package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import git4idea.fetch.GitFetchResult;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectKeyAvailabilityAssuranceAction;
import com.virtuslab.gitmachete.frontend.actions.common.GitFetchSupportImpl;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class FetchAllRemotesAction extends BaseProjectKeyAvailabilityAssuranceAction implements IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (GitFetchSupportImpl.fetchSupport(project).isFetchRunning()) {
      presentation.setEnabled(false);
      presentation.setDescription(getString("action.GitMachete.FetchAllRemotesAction.description.disabled.already-running"));
    } else {
      presentation.setDescription(getString("action.GitMachete.FetchAllRemotesAction.description"));
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);

    new Task.Backgroundable(project, getString("action.GitMachete.FetchAllRemotesAction.task-title"),
        /* canBeCancelled */ true) {
      @MonotonicNonNull
      GitFetchResult result = null;

      @Override
      public void run(ProgressIndicator indicator) {
        result = GitFetchSupportImpl.fetchSupport(project).fetchAllRemotes(gitRepository.toJavaList());
      }

      @Override
      public void onFinished() {
        var result = this.result;
        if (result != null) {
          result.showNotification();
        }
      }
    }.queue();
  }

}
