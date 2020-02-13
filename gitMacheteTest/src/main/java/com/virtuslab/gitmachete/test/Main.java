package com.virtuslab.gitmachete.test;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Main {
  public static void main(String[] argv) throws Exception {
    IGitMacheteRepository repo = null;

    try {
      repo =
          GitFactoryModule.getInjector()
              .getInstance(GitMacheteRepositoryFactory.class)
              .create(
                  Paths.get(System.getProperty("user.home"), "machete-sandbox"), Optional.empty());
    } catch (GitMacheteException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }

    var branch = repo.getRootBranches().get(0).getDownstreamBranches().get(0);

    /*var rp = branch.computeRebaseParameters();

    System.out.println(rp.getCurrentBranch().getName());
    System.out.println(rp.getNewBaseCommit().getMessage());
    System.out.println(rp.getNewBaseCommit().getHash());
    System.out.println(rp.getForkPointCommit().getMessage());
    System.out.println(rp.getForkPointCommit().getHash());*/

    printGitMacheteBranches(repo.getRootBranches(), 0);

    System.out.println("Current branch: ");
    repo.getCurrentBranchIfManaged()
        .ifPresentOrElse(b -> System.out.println(b.getName()), () -> System.out.println("EMPTY"));

    // printGitMacheteBranches(repo.getRootBranches(), 0);

    // System.out.println();

    // printGitMacheteBranches(repo.getRootBranches(), 0);

    /*IBranchRelationFile brf =
        GitFactoryModule.getInjector()
            .getInstance(BranchRelationFileFactory.class)
            .create(
                Paths.get(System.getProperty("user.home"), "machete-sandbox", ".git", "machete"));

    printRelationFileBranches(brf.getRootBranches(), 0);

    var brf2 = brf.slideOutBranchAndGetNewBranchRelationFileInstance("block-cancel-order");

    System.out.println();

    printRelationFileBranches(brf.getRootBranches(), 0);

    System.out.println();

    printRelationFileBranches(brf2.getRootBranches(), 0);*/

    /*var macheteFile =
        new BranchRelationFile(
            Paths.get(System.getProperty("user.home"), "machete-sandbox", ".git", "machete"));

    printBranches(macheteFile.getRootBranches(), 0);

    macheteFile.getRootBranches().get(0).getSubbranches().get(0).slideOut();
    System.out.println();

    printBranches(macheteFile.getRootBranches(), 0);

    macheteFile.saveToFile();*/

    // System.out.println(repo2.getCurrentBranch().get().getName());

    // repo2.getRootBranches().forEach(m -> System.out.println(m.getName()));

    /*var branches = repo.getRootBranches();

    for (var b : branches) {
      if (b.getName().equals("develop")) {
        for (var bb : b.getBranches()) {
          if (bb.getName().equals("allow-ownership-link")) {
            var fp = bb.getCoreLocalBranch().getForkPoint();
            System.out.println(fp);
          }
        }
      }
    }*/

    // System.out.println(repo.getSubmoduleRepositories());
  }

  private static void printRelationFileBranches(
      List<IBranchRelationFileEntry> branches, int level) {
    for (var branch : branches) {
      System.out.println(
          "\t".repeat(level) + branch.getName() + " # " + branch.getCustomAnnotation());
      printRelationFileBranches(branch.getSubbranches(), level + 1);
    }
  }

  private static void printGitMacheteBranches(List<IGitMacheteBranch> branches, int level) {
    for (var branch : branches) {
      System.out.print("\t".repeat(level) + branch.getName() + "(UPSTREAM: ");
      branch
          .getUpstreamBranch()
          .ifPresentOrElse(b -> System.out.print(b.getName()), () -> System.out.print("EMPTY"));
      System.out.println(" # " + branch.getCustomAnnotation());
      printGitMacheteBranches(branch.getDownstreamBranches(), level + 1);
    }
  }
}
