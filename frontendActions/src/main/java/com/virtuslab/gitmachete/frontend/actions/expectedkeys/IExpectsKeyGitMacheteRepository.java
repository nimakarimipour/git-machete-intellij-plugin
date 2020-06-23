package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepository> getGitMacheteRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY));
  }

  default Option<IGitMacheteRepository> getGitMacheteRepositoryLoggingOnEmpty(AnActionEvent anActionEvent) {
    var gitMacheteRepository = getGitMacheteRepository(anActionEvent);
    if (gitMacheteRepository.isEmpty()) {
      log().warn("Git Machete repository is undefined");
    }
    return gitMacheteRepository;
  }

  default Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    var branchLayout = getGitMacheteRepositoryLoggingOnEmpty(anActionEvent).flatMap(repository -> repository.getBranchLayout());
    if (branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default Option<IGitMacheteBranch> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    var gitMacheteBranch = getGitMacheteRepositoryLoggingOnEmpty(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());
    if (gitMacheteBranch.isEmpty()) {
      log().warn("Current Git Machete branch is undefined");
    }
    return gitMacheteBranch;
  }

  default Option<String> getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    var currentBranchName = getCurrentMacheteBranchIfManaged(anActionEvent).map(branch -> branch.getName());
    if (currentBranchName.isEmpty()) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default Option<IGitMacheteBranch> getGitMacheteBranchByName(AnActionEvent anActionEvent, String branchName) {
    var gitMacheteBranch = getGitMacheteRepositoryLoggingOnEmpty(anActionEvent).flatMap(r -> r.getBranchByName(branchName));
    if (gitMacheteBranch.isEmpty()) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return gitMacheteBranch;
  }
}
