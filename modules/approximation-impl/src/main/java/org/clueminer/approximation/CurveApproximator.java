package org.clueminer.approximation;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import org.clueminer.approximation.api.Approximator;
import org.clueminer.dataset.api.ContinuousInstance;
import org.clueminer.dataset.api.Instance;
import org.clueminer.dataset.api.Timeseries;
import org.clueminer.std.StdScale;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Barton
 */
@ServiceProvider(service = Approximator.class)
public class CurveApproximator extends Approximator {

    private static final String name = "curve-approx";
    private final String[] names = {"grow-start", "grow-period", "grow-angle", "dx-pos", "dx-y", "win-min", "winx-min", "win-max", "winx-max"};
    private Map<Integer, GrowingPeriod> subsequence;
    private int subSeq;
    private double delta = -0.05;
    private double[] xAxis;
    private double ymin = Double.NaN;
    private double ymax = Double.NaN;
    private StdScale sc = new StdScale();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void estimate(double[] xAxis, ContinuousInstance instance, HashMap<String, Double> coefficients) {
        double maxPeriod;
        int startId, endId;
        double x1, x2, y1, y2, dx;
        double max;
        int maxIdx = 0;
        this.xAxis = xAxis;

        max = Double.MIN_VALUE;

        x1 = xAxis[0];
        y1 = instance.value(0);
        startId = -1;
        endId = -1;
        subsequence = Maps.newHashMap();
        subSeq = 0;
        for (int i = 1; i < instance.size(); i++) {
            x2 = xAxis[i];
            y2 = instance.value(i);
            dx = (y2 - y1) / (x2 - x1);
            //check if derivative is positive -> ts is growing
            //System.out.println("dx =" + dx + " start: " + startId + " end: " + endId);
            if (startId == -1 && dx >= 0.0) {
                //growing started one point before, unless first point
                startId = i > 0 ? (i - 1) : i;
            } else if (dx < delta && endId == -1) {
                if (startId > -1) {
                    endId = i - 1;
                    addSubSequence(instance, startId, endId, subsequence);
                    startId = i;
                    endId = -1;
                } else {
                    startId = i;
                }
            }

            //find point with highest derivation
            if (dx > max) {
                max = dx;
                maxIdx = i;
            }

            //move to next point
            x1 = x2;
            y1 = y2;
        }
        //growing all the time
        if (endId == -1) {
            addSubSequence(instance, startId, instance.size() - 1, subsequence);
        }
        //find max subsequence
        maxPeriod = Double.MIN_VALUE;
        int maxId = -1;
        double diff;
        for (Map.Entry<Integer, GrowingPeriod> entry : subsequence.entrySet()) {
            diff = entry.getValue().getX2() - entry.getValue().getX1();
            if (diff > maxPeriod) {
                maxPeriod = diff;
                maxId = entry.getKey();
            }
        }

        if (Double.isNaN(ymin)) {
            Timeseries ts = instance.getParent();
            if (ts == null) {
                throw new RuntimeException("parent dataset is not set");
            }
            ymin = ts.getMin();
            ymax = ts.getMax();
        }

        robustDetection(xAxis, instance, coefficients);

        if (maxId > -1) {
            GrowingPeriod grow = subsequence.get(maxId);
            coefficients.put("grow-start", grow.x1);
            coefficients.put("grow-period", (grow.x2 - grow.x1));
            coefficients.put("grow-angle", growAngle(grow.x1, grow.x2, grow.y1, grow.y2));
            coefficients.put("dx-pos", xAxis[maxIdx]);
            coefficients.put("dx-y", instance.value(maxIdx));
        } else {
            throw new RuntimeException("no subseqence found");
        }
    }

    private double growAngle(double a1, double a2, double b1, double b2) {
        //standartize y values, so that we can compare angels
        double y1 = sc.scaleToRange(b1, ymin, ymax, 0.0, 1.0);
        double y2 = sc.scaleToRange(b2, ymin, ymax, 0.0, 1.0);
        double a = a2 - a1;
        double b = y2 - y1;
        //System.out.println("curve: [" + a1 + ", " + y1 + "] [" + a2 + ", " + y2 + "]");
        //System.out.println("curve: a = " + a + ", b = " + b + " tan= " + Math.atan((b / a)) + ", arg = " + (b / a));
        return Math.atan((b / a));
    }

    private void robustDetection(double[] xAxis, ContinuousInstance instance, HashMap<String, Double> coefficients) {
        double x1, x2, y1, y2, d, ay1, ay2;
        double min, max;
        int minIdx = 0, maxIdx;
        int window = 5;
        int j;

        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        for (int i = 0; i < (instance.size() - 2 * window); i++) {
            j = 0;
            y1 = 0;
            y2 = 0;
            while (j < window) {
                //if(inst.)
                y1 += instance.value(i + j);
                y2 += instance.value(i + j + window);
                j++;
            }
            ay1 = y1 / (double) window;
            ay2 = y2 / (double) window;

            x1 = (xAxis[i] - xAxis[i + window - 1]);
            x2 = (xAxis[i + window] - xAxis[i + 2 * window - 1]);
            d = (ay2 - ay1) / (x2 - x1);
            if (d < min) {
                min = d;
                //index should be even
                minIdx = i; //+ window / 2;
            }
            if (d > max) {
                max = d;
                maxIdx = i + window / 2;
            }
            //System.out.println("d " + i + " = " + d);
        }
        //paint min and max
        coefficients.put("win-min", instance.value(minIdx));
        coefficients.put("winx-min", xAxis[minIdx]);
        coefficients.put("win-max", instance.value(minIdx));
        coefficients.put("winx-max", xAxis[minIdx]);
    }

    private void addSubSequence(Instance inst, int startId, int endId, Map<Integer, GrowingPeriod> map) {
        double xs, xe;
        xs = xAxis[startId];
        xe = xAxis[endId];
        //System.out.println("found subseqence: " + xs + " - " + xe + " len: " + (xe - xs));
        int id = subSeq++;
        map.put(id, new GrowingPeriod(xs, xe, inst.value(startId), inst.value(endId)));
    }

    @Override
    public String[] getParamNames() {
        return names;
    }

    @Override
    public double getFunctionValue(double x, double[] coeff) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumCoefficients() {
        return names.length;
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    public double getYmax() {
        return ymax;
    }

    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    private class GrowingPeriod {

        private double x1, y1, x2, y2;

        public GrowingPeriod(double x1, double x2, double y1, double y2) {
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
        }

        public double getX1() {
            return x1;
        }

        public double getY1() {
            return y1;
        }

        public double getX2() {
            return x2;
        }

        public double getY2() {
            return y2;
        }

    }

}
