package org.clueminer.clustering.gui;

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import org.clueminer.gui.BottomBorder;
import org.clueminer.gui.ToolbarButton;

/**
 *
 * @author Tomas Barton
 */
public class ClusteringToolbar extends JToolBar implements Serializable {

    private static final long serialVersionUID = 1205882231518456747L;

    private final ClusterAnalysis parent;

    private ToolbarButton clusterBtn;
    private ToolbarButton exportBtn;
    private ToolbarButton printBtn;
    private ToolbarButton propertiesBtn;
    private ToolbarButton clusterExportBtn;
    private boolean showLabels = false;
    private boolean smallIcons = false;

    public ClusteringToolbar(ClusterAnalysis frame) {
        super("ClusteringToolbar", JToolBar.HORIZONTAL);
        parent = frame;
        initComponents();
        setFloatable(false);
        setBorder(new BottomBorder());
        addMouseListener(new ToolbarOptions(this));
    }

    private void initComponents() {
        add(clusterBtn = ToolbarButton.getButton(ClusterActions.clusterPopup(parent)));
        add(exportBtn = ToolbarButton.getButton(ClusterActions.exportImage(parent)));
        add(clusterExportBtn = ToolbarButton.getButton(ClusterActions.clusterExport(parent)));
        add(printBtn = ToolbarButton.getButton(ClusterActions.printChart(parent)));
        add(propertiesBtn = ToolbarButton.getButton(ClusterActions.chartProperties(parent)));

    }

    public void updateToolbar() {
    }

    public void toggleLabels() {
        showLabels = !showLabels;
        boolean show = showLabels;
        clusterBtn.toggleLabel(show);
        exportBtn.toggleLabel(show);
        clusterExportBtn.toggleLabel(show);
        printBtn.toggleLabel(show);
        propertiesBtn.toggleLabel(show);
    }

    public void toggleIcons() {
        smallIcons = !smallIcons;
        boolean small = smallIcons;
        clusterBtn.toggleIcon(small);
        exportBtn.toggleIcon(small);
        clusterExportBtn.toggleIcon(small);
        printBtn.toggleIcon(small);
        propertiesBtn.toggleIcon(small);
    }

    public JPopupMenu getToolbarMenu() {
        JPopupMenu popup = new JPopupMenu();
        JCheckBoxMenuItem item;

        popup.add(item = new JCheckBoxMenuItem(
                ClusterActions.toggleToolbarSmallIcons(parent, this)));
        item.setMargin(new Insets(0, 0, 0, 0));
        item.setState(smallIcons);

        popup.add(item = new JCheckBoxMenuItem(
                ClusterActions.toggleToolbarShowLabels(parent, this)));
        item.setMargin(new Insets(0, 0, 0, 0));
        item.setState(showLabels);

        return popup;
    }

    public static class ToolbarOptions extends MouseAdapter {

        private ClusteringToolbar toolbar;

        public ToolbarOptions(ClusteringToolbar bar) {
            toolbar = bar;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                toolbar.getToolbarMenu().show(toolbar, e.getX(), e.getY());
            }
        }
    }

}
