package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.algcomparison.statistic.utils.ArrowConfusion;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;

/**
 * Calculates the F1 statistic for arrowheads. See
 * </p>
 * https://en.wikipedia.org/wiki/F1_score
 * </p>
 * We use what's on this page called the "traditional" F1 statistic. If the true contains
 * X*->Y and estimated graph either does not contain an edge from X to Y or else does not
 * contain an arrowhead at X for an edge from X to Y, one false positive is counted.
 * Similarly for false negatives
 *
 * @author Joseh Ramsey
 */
public class F1ArrowCommonEdges implements Statistic {
    static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "F1ArrowC";
    }

    @Override
    public String getDescription() {
        return "F1 statistic for arrows for common edges";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        ArrowConfusion arrowConfusion = new ArrowConfusion(trueGraph, estGraph);
        int arrowTp = arrowConfusion.getArrowsTpc();
        int arrowFp = arrowConfusion.getArrowsFpc();
        int arrowFn = arrowConfusion.getArrowsFnc();
        int arrowTn = arrowConfusion.getArrowsTnc();
        double arrowPrecision = arrowTp / (double) (arrowTp + arrowFp);
        double arrowRecall = arrowTp / (double) (arrowTp + arrowFn);
        return 2 * (arrowPrecision * arrowRecall) / (arrowPrecision + arrowRecall);
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}