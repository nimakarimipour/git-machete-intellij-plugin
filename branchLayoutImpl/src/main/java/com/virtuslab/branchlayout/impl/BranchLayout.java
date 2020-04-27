package com.virtuslab.branchlayout.impl;

import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import lombok.Data;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

@Data
public class BranchLayout implements IBranchLayout {
  private static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("branchLayout");

  private final List<BaseBranchLayoutEntry> rootBranches;

  @Override
  public Option<BaseBranchLayoutEntry> findEntryByName(String branchName) {
    return findEntryRecursively(getRootBranches(), e -> e.getName().equals(branchName));
  }

  @Override
  public IBranchLayout slideOut(String branchName) throws BranchLayoutException {
    var entryOption = findEntryByName(branchName);
    if (entryOption.isEmpty()) {
      throw new BranchLayoutException("Branch entry '${branchName}' does not exist");
    }
    if (rootBranches.contains(entryOption.get())) {
      throw new BranchLayoutException("Cannot slide out root branch entry");
    }
    return slideOut(entryOption.get());
  }

  /** @return {@link IBranchLayout} where given {@code entryToSlideOut} is replaced with entries of its subbranches */
  private IBranchLayout slideOut(BaseBranchLayoutEntry entryToSlideOut) throws BranchLayoutException {
    LOG.debug(() -> "Enter slideOut for entry ${entryToSlideOut.getName()}");

    var upstreamEntryOption = findUpstreamEntryForEntry(entryToSlideOut);
    if (upstreamEntryOption.isEmpty()) {
      throw new BranchLayoutException("Can't find upstream for entry ${entryToSlideOut.getName()}");
    }
    var upstream = upstreamEntryOption.get();
    LOG.debug(() -> "Upstream for ${entryToSlideOut.getName()} is ${upstream.getName()}");

    int indexInUpstream = upstream.getSubentries().indexOf(entryToSlideOut);
    LOG.debug(() -> "Entry ${entryToSlideOut.getName()} has index ${indexInUpstream} in its upstream");

    LOG.debug("Removing this entry from upstream and setting this entry's parent as new parent " +
        "for this branch's downstreams");

    var updatedSubbranches = upstream.getSubentries()
        .removeAt(indexInUpstream)
        .insertAll(indexInUpstream, entryToSlideOut.getSubentries());

    LOG.debug("Following subentries has been updated:");
    entryToSlideOut.getSubentries().forEach(downstream -> LOG.debug(() -> "* ${downstream} (${downstream.getName()})"));

    var updatedUpstream = updateSubbranchesForEntry(upstream, updatedSubbranches);

    return replace(upstream, updatedUpstream);
  }

  /**
   * @return a {@link IBranchLayout} containing all elements of this where the {@code entry} is replaced with
   *         {@code newEntry}
   */
  private IBranchLayout replace(BaseBranchLayoutEntry oldEntry, BaseBranchLayoutEntry newEntry) {
    LOG.debug(() -> "Enter replace(oldEntry = ${oldEntry} (${oldEntry.getName()}), " +
        "newEntry = ${newEntry} (${newEntry.getName()})");
    if (rootBranches.contains(oldEntry)) {
      LOG.debug("Old entry is one of the root entries. Replacing.");
      return new BranchLayout(rootBranches.replace(oldEntry, newEntry));
    } else {
      LOG.debug("Old entry is one of subentries. Finding upstream.");

      var entryUpstreamOption = findUpstreamEntryForEntry(oldEntry);
      assert entryUpstreamOption.isDefined() : "Upstream is not defined";
      var upstreamEntry = entryUpstreamOption.get();

      LOG.debug(() -> "Upstream for old entry is ${upstreamEntry} (${upstreamEntry.getName()})");

      LOG.debug("Updating subentries");
      var updatedSubentries = upstreamEntry.getSubentries().replace(oldEntry, newEntry);
      LOG.debug("Updated subentries:");
      updatedSubentries.forEach(entry -> LOG.debug(() -> "* ${entry} (${entry.getName()})"));
      var updatedUpstreamEntry = updateSubbranchesForEntry(upstreamEntry, updatedSubentries);

      return replace(upstreamEntry, updatedUpstreamEntry);
    }
  }

  /** @return a copy of the {@code entry} but with specified {@code subbranches} list */
  private BaseBranchLayoutEntry updateSubbranchesForEntry(BaseBranchLayoutEntry entry,
      List<BaseBranchLayoutEntry> subbranches) {
    var name = entry.getName();
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    return new BranchLayoutEntry(name, customAnnotation, subbranches);
  }

  private Option<BaseBranchLayoutEntry> findUpstreamEntryForEntry(BaseBranchLayoutEntry entry) {
    return findEntryRecursively(rootBranches, e -> e.getSubentries().contains(entry));
  }

  /** Recursively traverses the list for an element that satisfies the {@code predicate}. */
  private static Option<BaseBranchLayoutEntry> findEntryRecursively(
      List<BaseBranchLayoutEntry> entries,
      Predicate<BaseBranchLayoutEntry> predicate) {
    LOG.debug(() -> "Enter findEntryRecursively(entries = ${entries}, predicate = ${predicate})");

    for (var entry : entries) {
      LOG.debug(() -> "Testing entry ${entry} (${entry.getName()})");
      if (predicate.test(entry)) {
        LOG.debug(() -> "Entry ${entry} (${entry.getName()}) satisfies predicate. Returning.");
        return Option.of(entry);
      }

      LOG.debug("Entry not found on this level. Searching in sublevel.");
      var result = findEntryRecursively(entry.getSubentries(), predicate);
      if (result.isDefined()) {
        return result;
      }
    }

    LOG.debug("Entry satisfies the predicate not found on this level neither its sublevels");
    return Option.none();
  }
}
