package org.clueminer.clustering.confusion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.font.FontRenderContext;
import org.clueminer.clustering.api.Cluster;

/**
 *
 * @author Tomas Barton
 */
public class RowLabels extends AbstractLabels {

    private static final long serialVersionUID = -1771575720458918717L;

    private final Insets insets = new Insets(0, 5, 0, 0);

    @Override
    protected void render(Graphics2D g) {
        if (hasData()) {
            g.setColor(Color.black);
            float annY;
            g.setFont(defaultFont);
            FontRenderContext frc = g.getFontRenderContext();
            FontMetrics fm = g.getFontMetrics();
            int ascent = fm.getMaxAscent();
            int descent = fm.getDescent();
            String s;
            /*
             * Fonts are not scaling lineraly

             *---------------ascent
             *
             * FONT
             * ----- baseline
             *
             * --------------descent
             *
             */
            Cluster curr;
            maxWidth = 0;
            double offset = (elementSize.height / 2.0) + ((ascent - descent) / 2.0);
            for (int row = 0; row < rowData.size(); row++) {
                curr = rowData.get(row);
                annY = (float) (row * elementSize.height + offset);
                s = curr.getName();
                if (s == null) {
                    s = unknownLabel;
                }

                int width = (int) (g.getFont().getStringBounds(s, frc).getWidth());
                checkMax(width);
                g.drawString(s, insets.left, annY);
            }
            if (changedMax) {
                changedMax = false;
                recalculate();
            }
        }
    }

    @Override
    protected void recalculate() {
        int width = 10 + maxWidth + insets.left + insets.right;
        int height;
        if (elementSize.height < lineHeight) {
            //no need to display unreadable text
            visible = false;
            width = 0;
            height = 0;
            bufferedImage = null;
        } else {
            visible = true;
            height = elementSize.height * rowData.size() + 1;

        }
        this.size.width = width;
        this.size.height = height;
        double fsize = elementSize.width * 0.1;
        defaultFont = defaultFont.deriveFont((float) fsize);
        //System.out.println("row labels size: " + size.toString());
        setMinimumSize(size);
        setSize(size);
        setPreferredSize(size);
    }

    @Override
    protected void updateSize(Dimension size) {
        elementSize = size;
        resetCache();
    }

    /**
     * Check is clustering A is not empty
     *
     * @return true when has data to render component
     */
    @Override
    public boolean hasData() {
        return (rowData != null);
    }

}