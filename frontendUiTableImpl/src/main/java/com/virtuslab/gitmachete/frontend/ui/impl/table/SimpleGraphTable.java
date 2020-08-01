package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRendererComponent;

public final class SimpleGraphTable extends JBTable implements IGitMacheteRepositorySnapshotProvider {
  @Getter
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot = NullGitMacheteRepositorySnapshot.getInstance();

  @UIEffect
  public static SimpleGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot) {
    // We can keep the data - graph table model,
    // but wee need to reinstantiate the UI - demo graph table.
    return new SimpleGraphTable(deriveGraphTableModel(macheteRepositorySnapshot));
  }

  @UIEffect
  private static GraphTableModel deriveGraphTableModel(IGitMacheteRepositorySnapshot macheteRepositorySnapshot) {
    var repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    var repositoryGraph = repositoryGraphCache.getRepositoryGraph(macheteRepositorySnapshot, /* isListingCommits */ true);
    return new GraphTableModel(repositoryGraph);
  }

  @UIEffect
  private SimpleGraphTable(GraphTableModel graphTableModel) {
    super(graphTableModel);

    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);

    setDefaultRenderer(BranchOrCommitCell.class, BranchOrCommitCellRendererComponent::new);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);
  }
}