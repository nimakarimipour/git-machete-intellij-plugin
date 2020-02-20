package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommit;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRemoteBranch;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

public class JGitLocalBranch extends JGitBranch implements IGitCoreLocalBranch {
  public static final String branchesPath = "refs/heads/";

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
    return getBranchTypeString(/*capitalized*/ false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Local" : "local";
  }

  @Override
  public Optional<IGitCoreBranchTrackingStatus> getRemoteTrackingStatus() throws JGitException {
    BranchTrackingStatus ts;
    try {
      ts = BranchTrackingStatus.of(repo.getJgitRepo(), getName());
    } catch (IOException e) {
      throw new JGitException(e);
    }

    if (ts == null) return Optional.empty();

    return Optional.of(JGitBranchTrackingStatus.of(ts.getAheadCount(), ts.getBehindCount()));
  }

  @Override
  public Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    var bc = new BranchConfig(repo.getJgitRepo().getConfig(), getName());
    String remoteName = bc.getRemoteTrackingBranch();
    if (remoteName == null) return Optional.empty();
    else
      return Optional.of(
          new JGitRemoteBranch(repo, remoteName.substring(JGitRemoteBranch.branchesPath.length())));
  }

  @Override
  public Optional<IGitCoreCommit> getForkPoint() throws GitException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = getPointedRevCommit();
    try {
      walk.markStart(commit);
    } catch (Exception e) {
      throw new JGitException(e);
    }

    List<List<ReflogEntry>> reflogEntriesList = new LinkedList<>();

    for (var branch : this.repo.getLocalBranches()) {
      if (!branch.equals(this)) {
        try {
          reflogEntriesList.add(
              repo.getJgitRepo().getReflogReader(branch.getFullName()).getReverseEntries());
        } catch (Exception e) {
          throw new JGitException(e);
        }
      }
    }

    Optional<IGitCoreRemoteBranch> remoteTrackingBranch = getRemoteTrackingBranch();

    for (var branch : this.repo.getRemoteBranches()) {
      if (remoteTrackingBranch.filter(branch::equals).isEmpty()) {
        try {
          reflogEntriesList.add(
              repo.getJgitRepo().getReflogReader(branch.getFullName()).getReverseEntries());
        } catch (Exception e) {
          throw new JGitException(e);
        }
      }
    }

    // Filter reflogs
    // Note: Machete CLI do this in a little different way: it exclude also all reflog entries that
    // have the same NewId as entries that starts with "branch: Reset to" or "reset: moving to"
    // See: https://github.com/VirtusLab/git-machete/pull/73
    for (var entries : reflogEntriesList) {
      var firstEntryNewID =
          entries.size() > 0 ? entries.get(entries.size() - 1).getNewId() : ObjectId.zeroId();
      entries.removeIf(
          e ->
              e.getNewId().equals(firstEntryNewID)
                  || e.getNewId().equals(e.getOldId())
                  || e.getComment().startsWith("branch: Reset to ")
                  || e.getComment().startsWith("reset: moving to "));
    }

    for (var curBranchCommit : walk) {
      for (var branchReflog : reflogEntriesList) {
        for (var branchReflogEntry : branchReflog) {
          if (curBranchCommit.getId().equals(branchReflogEntry.getNewId())) {
            return Optional.of(new JGitCommit(curBranchCommit, repo));
          }
        }
      }
    }

    return Optional.empty();
  }
}
