package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRemoteBranch;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;

public class JGitLocalBranch extends JGitBranch implements IGitCoreLocalBranch {
  public static String branchesPath = "refs/heads/";

  public JGitLocalBranch(JGitRepository repo, String branchName) {
    super(repo, branchName);
  }

  @Override
  public String getBranchesPath() {
    return branchesPath;
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public String getBranchTypeString() {
    return getBranchTypeString(false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    if (capitalized) return "Local";
    else return "local";
  }

  @Override
  public Optional<IGitCoreBranchTrackingStatus> getTrackingStatus() throws JGitException {
    BranchTrackingStatus ts;
    try {
      ts = BranchTrackingStatus.of(repo.getJgitRepo(), getName());
    } catch (IOException e) {
      throw new JGitException(e);
    }

    if (ts == null) return Optional.empty();

    return Optional.of(JGitBranchTrackingStatus.build(ts.getAheadCount(), ts.getBehindCount()));
  }

  @Override
  public Optional<IGitCoreRemoteBranch> getTrackingBranch() {
    var bc = new BranchConfig(repo.getJgitRepo().getConfig(), getName());
    String remoteName = bc.getTrackingBranch();
    if (remoteName == null) return Optional.empty();
    else
      return Optional.of(
          new JGitRemoteBranch(repo, remoteName.substring(JGitRemoteBranch.branchesPath.length())));
  }
}
