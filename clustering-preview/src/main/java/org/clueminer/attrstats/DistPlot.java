package org.clueminer.attrstats;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Arrays;
import javax.swing.JPanel;
import org.clueminer.dataset.api.Attribute;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.stats.AttrNumStats;
import org.clueminer.std.StdScale;
import org.clueminer.utils.Dump;

/**
 *
 * @author Tomas Barton
 */
public class DistPlot extends JPanel {

    private static final long serialVersionUID = 5461063176271490884L;
    private final Insets insets = new Insets(10, 10, 10, 10);
    private int width = 200;
    private BufferedImage bufferedImage;
    private Graphics2D bufferedGraphics;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private Dataset<? extends Instance> dataset;
    private StdScale scale = new StdScale();
    private int height = 200;
    private int diameter = 4;
    private int barSize = 100;

    public DistPlot() {
        setDoubleBuffered(false);

        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                bufferedImage = null;
                height = getSize().height;
                width = (int) (getSize().width * 0.6);
                repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
        setSize(new Dimension(200, height));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (dataset == null) {
            return;
        }

        int plotHeight = this.getHeight() - insets.bottom - insets.top;
        if (plotHeight < 10) {
            //default height which is not bellow zero
            plotHeight = 20;
        }
        //create color palette
        if (bufferedImage == null) {
            drawData(width, plotHeight);
        }
        //places color bar to canvas
        g2d.drawImage(bufferedImage,
                      insets.left, insets.top,
                      width, plotHeight,
                      null);

        int totalWidth = insets.left + width + barSize + insets.right;
        int totalHeight = insets.top + plotHeight + insets.bottom;
        setMinimumSize(new Dimension(totalWidth, totalHeight));
    }

    private void drawData(int plotWidth, int plotHeight) {
        bufferedImage = new BufferedImage(plotWidth, plotHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Attribute attr = dataset.getAttribute(0);
        Ellipse2D.Double circle;

        double max = attr.statistics(AttrNumStats.MAX);
        double avg = attr.statistics(AttrNumStats.AVG);
        double min = attr.statistics(AttrNumStats.MIN);
        //   double stdDev = attr.statistics(AttrNumStats.STD_DEV);

        System.out.println("max = " + max);
        System.out.println("min = " + min);
        System.out.println("avg = " + avg);

        double[] values = attr.asDoubleArray();
        Arrays.sort(values);
        Dump.array(values, "sorted array");
        int size = dataset.size();
        double median;
        if (size % 2 == 0) {
            median = values[values.length / 2] + values[values.length / 2 + 1] / 2;
        } else {
            median = values[values.length / 2];
        }
        System.out.println("median = " + median);

        double x, y;
        x = diameter + 5;
        g2.setColor(Color.blue);
        for (int i = 0; i < values.length; i++) {
            y = scale(values[i], min, max);

            circle = new Ellipse2D.Double(x - diameter / 2.0, y - diameter / 2.0, diameter, diameter);
            g2.fill(circle);
        }

        int xPlot = 50;
        int tickLength = 10;
        int q1y = (int) scale(quartile(values, 25), min, max);
        int q3y = (int) scale(quartile(values, 75), min, max);

        g2.setColor(Color.BLACK);
        g2.drawRect(xPlot, q3y, barSize, (q1y - q3y));

        //min tick
        int yTick = (int) scale(min, min, max);
        g2.setColor(Color.black);
        int xTick = xPlot + (barSize / 2) - (tickLength / 2);
        g2.drawLine(xTick, yTick, xTick + (tickLength / 2), yTick);

        //min Label
        String label;
        label = decimalFormat.format(min);
        g2.drawString(label, xTick, yTick);

        Stroke drawingStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Line2D line = new Line2D.Double(xTick, yTick, xTick, q1y);

        Stroke oldStroke = g2.getStroke();

        //connect min and q1
        g2.setStroke(drawingStroke);
        g2.draw(line);

        yTick = (int) scale(max, min, max);
        line = new Line2D.Double(xTick, yTick, xTick, q3y);
        g2.draw(line);

        //unset stroke
        g2.setStroke(oldStroke);
        // max tick
        yTick = plotHeight;
        g2.setColor(Color.black);
        g2.drawLine(xTick, yTick, xPlot + tickLength, yTick);

        // avg tick
        yTick = (int) scale(avg, min, max);
        g2.drawLine(xPlot, yTick, xPlot + barSize, yTick);

        drawMedian(g2, median, xPlot, min, max);

        g2.dispose();
    }

    private void drawMedian(Graphics2D g2, double median, double xPlot, double min, double max) {
        // median tick
        double yMedian = scale(median, min, max);
        g2.setColor(Color.RED);
        Line2D line = new Line2D.Double(xPlot, yMedian, xPlot + barSize, yMedian);
        g2.draw(line);

        String label;
        label = decimalFormat.format(median);
        g2.drawString(label, (int) (xPlot + barSize / 2), ((int) yMedian) - 1);

    }

    /**
     * Min should start at bottom, so we revert all heights
     *
     * @param v
     * @param min
     * @param max
     * @return
     */
    private double scale(double v, double min, double max) {
        return height - scale.scaleToRange(v, min, max, 0, height);
    }

    /**
     * Commonly used are 25 (Q1) and 75(Q2)
     *
     * @param values
     * @param lowerPercent
     * @return
     */
    public double quartile(double[] values, double lowerPercent) {
        int n = (int) Math.round(values.length * lowerPercent / 100);

        return values[n];
    }

    public Dataset<? extends Instance> getDataset() {
        return dataset;
    }

    public void setDataset(Dataset<? extends Instance> dataset) {
        this.dataset = dataset;
    }

}