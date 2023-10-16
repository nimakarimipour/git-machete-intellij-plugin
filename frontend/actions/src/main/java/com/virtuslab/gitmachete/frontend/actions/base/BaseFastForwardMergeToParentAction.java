package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMerge;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.qual.async.ContinuesInBackground;

public abstract class BaseFastForwardMergeToParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseFastForwardMergeToParentAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseFastForwardMergeToParentAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.InSync, SyncToParentStatus.InSyncButForkPointOff);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val stayingBranchName = getNameOfBranchUnderAction(anActionEvent);
    if (gitRepository == null || stayingBranchName == null) {
      return;
    }

    val stayingBranch = getManagedBranchByName(anActionEvent, stayingBranchName);
    if (stayingBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert stayingBranch.isNonRoot() : "Branch that would be fast-forwarded TO is a root";

    val nonRootStayingBranch = stayingBranch.asNonRoot();
    val mergeProps = new MergeProps(
        /* movingBranch */ nonRootStayingBranch.getParent(),
        /* stayingBranch */ nonRootStayingBranch);
    new FastForwardMerge(gitRepository, mergeProps, /* fetchNotificationTextPrefix */ "").run();
  }
}
