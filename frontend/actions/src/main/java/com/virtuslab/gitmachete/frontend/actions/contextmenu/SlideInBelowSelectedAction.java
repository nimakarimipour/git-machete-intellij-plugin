package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideInBelowAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class SlideInBelowSelectedAction extends BaseSlideInBelowAction
    implements
      IExpectsKeySelectedBranchName {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}