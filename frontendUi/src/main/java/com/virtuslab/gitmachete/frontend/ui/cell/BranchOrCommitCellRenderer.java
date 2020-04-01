package com.virtuslab.gitmachete.frontend.ui.cell;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JTable;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.graph.EdgePrintElement;
import com.intellij.vcs.log.graph.NodePrintElement;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.PaintParameters;
import com.intellij.vcs.log.ui.render.LabelPainter;
import com.intellij.vcs.log.ui.render.TypeSafeTableCellRenderer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.SyncToOriginStatusToTextColorMapper;
import com.virtuslab.gitmachete.frontend.graph.elements.BranchElement;
import com.virtuslab.gitmachete.frontend.graph.elements.CommitElement;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.labeling.SyncToOriginStatusLabelGenerator;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTable;

public class BranchOrCommitCellRenderer extends TypeSafeTableCellRenderer<BranchOrCommitCell> {

  private final MyComponent myComponent;

  public BranchOrCommitCellRenderer(GitMacheteGraphTable table, GraphCellPainter painter) {
    myComponent = new MyComponent(table, painter);
  }

  @Override
  protected SimpleColoredComponent getTableCellRendererComponentImpl(
      JTable table,
      BranchOrCommitCell value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column) {
    myComponent.customize(value, isSelected, hasFocus, row, column);
    return myComponent;
  }

  @RequiredArgsConstructor
  private static class MyComponent extends SimpleColoredRenderer {
    private final GitMacheteGraphTable graphTable;
    private final GraphCellPainter painter;

    // Note: using deprecated `UIUtil.createImage` instead of `ImageUtil.createImage` to maintain compatibility with
    // IntelliJ platform 2019.2
    GraphImage graphImage = new GraphImage(UIUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0);

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      Graphics2D g2d = (Graphics2D) g;
      // The image's origin (after the graphics translate is applied) is rounded by J2D with .5
      // coordinate ceil'd.
      // This doesn't correspond to how the rectangle's origin is rounded, with .5 floor'd. As the
      // result, there may be a gap
      // b/w the background's top and the image's top (depending on the row number and the graphics
      // translate). To avoid that,
      // the graphics y-translate is aligned to int with .5-floor-bias.
      AffineTransform origTx = PaintUtil.alignTxToInt(/* graphics2d */ g2d, /* offset */ null, /* alignX */ false,
          /* alignY */ true, PaintUtil.RoundingMode.ROUND_FLOOR_BIAS);
      UIUtil.drawImage(g, graphImage.getImage(), 0, 0, null);
      if (origTx != null) {
        g2d.setTransform(origTx);
      }
    }

    // For setBorder(null)
    @SuppressWarnings("argument.type.incompatible")
    void customize(BranchOrCommitCell cell, boolean isSelected, boolean hasFocus, int row, int column) {
      clear();
      setPaintFocusBorder(false);
      acquireState(graphTable, isSelected, hasFocus, row, column);
      getCellState().updateRenderer(this);
      setBorder(null);

      IGraphElement element = cell.getElement();

      if (element.hasBulletPoint()) {
        graphImage = getGraphImage(cell.getPrintElements());
      } else {
        graphImage = getGraphImage(cell.getPrintElements().stream().filter(e -> !(e instanceof NodePrintElement))
            .collect(Collectors.toList()));
      }

      append(""); // appendTextPadding won't work without this

      int width = calculateTextPadding(element);
      appendTextPadding(width);
      SimpleTextAttributes attributes = element.getAttributes();
      append(cell.getText(), attributes);

      if (element instanceof BranchElement) {
        BaseGitMacheteBranch branch = ((BranchElement) element).getBranch();
        Optional<String> customAnnotation = branch.getCustomAnnotation();
        if (customAnnotation.isPresent()) {
          append("   " + customAnnotation.get(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }

        SyncToOriginStatus syncToOriginStatus;

        syncToOriginStatus = ((BranchElement) element).getSyncToOriginStatus();
        if (syncToOriginStatus != SyncToOriginStatus.InSync) {
          SimpleTextAttributes textAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
              SyncToOriginStatusToTextColorMapper.getColor(syncToOriginStatus.getId()));
          append("  (" + SyncToOriginStatusLabelGenerator.getLabel(syncToOriginStatus.getId()) + ")",
              textAttributes);
        }
      }
    }

    /*
     * TODO (#98): The padding is calculated for all commits in the same branch (and the branch itself). The time
     * consumed could be reduced by using some lazy value, caching, or indent storing.
     */
    private int calculateTextPadding(IGraphElement element) {
      int width = graphImage.getWidth();
      if (element instanceof CommitElement) {
        RepositoryGraph repositoryGraph = graphTable.getModel().getRepositoryGraph();
        Collection<? extends PrintElement> printElements = repositoryGraph
            .getPrintElements(((CommitElement) element).getBranchElementIndex());
        double maxIndex = getMaxGraphElementIndex(printElements);
        width = (int) (maxIndex * PaintParameters.getNodeWidth(graphTable.getRowHeight()));
      }
      return width + LabelPainter.RIGHT_PADDING.get();
    }

    private GraphImage getGraphImage(Collection<? extends PrintElement> printElements) {
      double maxIndex = getMaxGraphElementIndex(printElements);
      BufferedImage image = UIUtil.createImage(graphTable.getGraphicsConfiguration(),
          (int) (PaintParameters.getNodeWidth(graphTable.getRowHeight()) * (maxIndex + 2)), graphTable.getRowHeight(),
          BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.CEIL);
      Graphics2D g2 = image.createGraphics();
      painter.draw(g2, printElements);

      int width = (int) (maxIndex * PaintParameters.getNodeWidth(graphTable.getRowHeight()));
      return new GraphImage(image, width);
    }

    private double getMaxGraphElementIndex(Collection<? extends PrintElement> printElements) {
      double maxIndex = 0;
      for (PrintElement printElement : printElements) {
        maxIndex = Math.max(maxIndex, printElement.getPositionInCurrentRow());
        if (printElement instanceof EdgePrintElement) {
          maxIndex = Math.max(maxIndex,
              (printElement.getPositionInCurrentRow() + ((EdgePrintElement) printElement).getPositionInOtherRow())
                  / 2.0);
        }
      }
      maxIndex++;
      return maxIndex;
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
      return graphTable.getFontMetrics(font);
    }
  }

  @AllArgsConstructor
  @Getter
  private static class GraphImage {
    private final Image image;
    private final int width;
  }
}