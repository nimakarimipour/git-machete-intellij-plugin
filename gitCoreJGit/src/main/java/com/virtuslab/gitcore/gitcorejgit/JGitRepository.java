package com.virtuslab.gitcore.gitcorejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.GitNoSuchBranchException;
import com.virtuslab.gitcore.gitcoreapi.GitNoSuchRepositoryException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRepository;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreSubmoduleEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;

@Getter
public class JGitRepository implements IGitCoreRepository {
  private final Repository jgitRepo;
  private final Git jgitGit;
  private final Path repositoryPath;
  private final Path gitFolderPath;

  @Getter(AccessLevel.NONE)
  private final Pattern gitDirPattern = Pattern.compile("gitdir:\\s*(.*)");

  @Inject
  public JGitRepository(@Assisted Path repositoryPath)
      throws IOException, GitNoSuchRepositoryException {
    this.repositoryPath = repositoryPath;
    Path gitPath = repositoryPath.resolve(".git");

    if (Files.isDirectory(gitPath)) {
      this.gitFolderPath = gitPath;
    } else if (Files.isRegularFile(gitPath)) {
      this.gitFolderPath = getGitFolderPathFromGitFile(gitPath);
    } else {
      throw new GitNoSuchRepositoryException(
          MessageFormat.format("Repository in path \"{0}\" does not exists", repositoryPath));
    }

    jgitRepo = new FileRepository(this.gitFolderPath.toString());
    jgitGit = new Git(jgitRepo);
  }

  private Path getGitFolderPathFromGitFile(Path gitFilePath)
      throws IOException, GitNoSuchRepositoryException {
    String gitFile = Files.readString(gitFilePath);
    Matcher matcher = gitDirPattern.matcher(gitFile);
    if (matcher.find()) {
      return gitFilePath.getParent().resolve(matcher.group(1)).normalize();
    }

    throw new GitNoSuchRepositoryException(
        MessageFormat.format("Path \"{0}\" does not contain any submodule", this.repositoryPath));
  }

  @Override
  public Optional<IGitCoreLocalBranch> getCurrentBranch() throws JGitException {
    Ref r;
    try {
      r = jgitRepo.getRefDatabase().findRef(Constants.HEAD);
    } catch (IOException e) {
      throw new JGitException("Cannot get current branch", e);
    }

    if (r == null) throw new JGitException("Error occur while getting current branch ref");

    if (r.isSymbolic())
      return Optional.of(
          new JGitLocalBranch(this, Repository.shortenRefName(r.getTarget().getName())));

    return Optional.empty();
  }

  @Override
  public JGitLocalBranch getLocalBranch(String branchName) throws GitException {
    if (branchIsMissing(JGitLocalBranch.branchesPath + branchName)) {
      throw new GitNoSuchBranchException(
          MessageFormat.format(
              "Local branch \"{0}\" does not exist in this repository", branchName));
    }
    return new JGitLocalBranch(/*repo*/ this, branchName);
  }

  @Override
  public JGitRemoteBranch getRemoteBranch(String branchName) throws GitException {
    if (branchIsMissing(JGitRemoteBranch.branchesPath + branchName)) {
      throw new GitNoSuchBranchException(
          MessageFormat.format(
              "Remote branch \"{0}\" does not exist in this repository", branchName));
    }
    return new JGitRemoteBranch(/*repo*/ this, branchName);
  }

  @Override
  public List<IGitCoreLocalBranch> getLocalBranches() throws GitException {
    List<IGitCoreLocalBranch> list = new LinkedList<>();
    try {
      for (Ref ref : this.getJgitGit().branchList().call()) {
        list.add(
            new JGitLocalBranch(
                /*repo*/ this, ref.getName().replace(JGitLocalBranch.branchesPath, "")));
      }
    } catch (GitAPIException e) {
      throw new JGitException("Error while getting list of local branches", e);
    }

    return list;
  }

  @Override
  public List<IGitCoreRemoteBranch> getRemoteBranches() throws GitException {
    List<IGitCoreRemoteBranch> list = new LinkedList<>();
    try {
      for (Ref ref :
          this.getJgitGit().branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()) {
        list.add(
            new JGitRemoteBranch(
                /*repo*/ this, ref.getName().replace(JGitRemoteBranch.branchesPath, "")));
      }
    } catch (GitAPIException e) {
      throw new JGitException("Error while getting list of remote branches", e);
    }

    return list;
  }

  @Override
  public List<IGitCoreSubmoduleEntry> getSubmodules() throws JGitException {
    SubmoduleWalk sWalk;
    try {
      sWalk = SubmoduleWalk.forIndex(this.jgitRepo);
    } catch (IOException e) {
      throw new JGitException("Error while initializing submodule walk", e);
    }

    List<IGitCoreSubmoduleEntry> submodules = new LinkedList<>();

    try {
      while (sWalk.next()) {
        submodules.add(
            new JGitSubmoduleEntry(sWalk.getDirectory().toPath(), sWalk.getModuleName()));
      }
    } catch (IOException e) {
      throw new JGitException("Error while fetching next submodule", e);
    }

    return submodules;
  }

  private boolean branchIsMissing(String path) throws JGitException {
    try {
      ObjectId o = jgitRepo.resolve(path);
      return o == null;
    } catch (RevisionSyntaxException | IOException e) {
      throw new JGitException(e);
    }
  }
}
