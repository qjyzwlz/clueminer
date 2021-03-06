package org.clueminer.eval.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.ClusterEvaluation;
import org.clueminer.clustering.api.InternalEvaluator;
import org.clueminer.clustering.api.factory.InternalEvaluatorFactory;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.clustering.api.EvaluationTable;
import org.clueminer.clustering.api.ExternalEvaluator;
import org.clueminer.clustering.api.factory.ExternalEvaluatorFactory;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.utils.Props;

/**
 *
 * @author Tomas Barton
 */
public class HashEvaluationTable implements EvaluationTable {

    private Clustering<Cluster> clustering;
    private Dataset<? extends Instance> dataset;
    protected static Object2ObjectMap<String, ClusterEvaluation> internalMap;
    protected static Object2ObjectMap<String, ClusterEvaluation> externalMap;
    private HashMap<String, Double> scores;

    public HashEvaluationTable(Clustering<? extends Cluster> clustering, Dataset<? extends Instance> dataset) {
        initEvaluators();
        setData(clustering, dataset);
    }

    @Override
    public final void setData(Clustering<? extends Cluster> clustering, Dataset<? extends Instance> dataset) {
        this.clustering = (Clustering<Cluster>) clustering;
        this.dataset = dataset;
        reset();
    }

    private void reset() {
        scores = new HashMap<>(internalMap.size() + externalMap.size());
    }

    @Override
    public HashMap<String, Double> getAll() {
        return scores;
    }

    @Override
    public HashMap<String, Double> countAll() {
        for (ClusterEvaluation eval : internalMap.values()) {
            getScore(eval);
        }
        for (ClusterEvaluation eval : externalMap.values()) {
            getScore(eval);
        }
        return getAll();
    }

    @Override
    public double getScore(ClusterEvaluation evaluator, Props params) {
        String key = evaluator.getName();
        if (scores.containsKey(key)) {
            return scores.get(key);
        } else {
            double score = evaluator.score(clustering, params);
            scores.put(key, score);
            return score;
        }
    }

    @Override
    public double getScore(String evaluator, Props params) {
        if (internalMap.containsKey(evaluator)) {
            return getScore(internalMap.get(evaluator), params);
        } else if (externalMap.containsKey(evaluator)) {
            return getScore(externalMap.get(evaluator), params);
        } else {
            throw new RuntimeException("unknown evaluator");
        }
    }

    /**
     * Computes evaluator score and caches the result
     *
     * @param evaluator
     * @return
     */
    @Override
    public double getScore(ClusterEvaluation evaluator) {
        return getScore(evaluator, new Props());
    }

    @Override
    public double getScore(String evaluator) {
        return getScore(evaluator, new Props());
    }

    private Map<String, Double> evalToScoreMap(Object2ObjectMap<String, ClusterEvaluation> map) {
        HashMap<String, Double> res = new HashMap<>(map.size());
        for (ClusterEvaluation eval : map.values()) {
            res.put(eval.getName(), getScore(eval));
        }
        return res;
    }

    @Override
    public Map<String, Double> getInternal() {
        return evalToScoreMap(internalMap);
    }

    @Override
    public Map<String, Double> getExternal() {
        return evalToScoreMap(externalMap);
    }

    @Override
    public String[] getEvaluators() {
        if (internalMap == null) {
            return new String[0];
        }
        Collection<String> evaluators = new TreeSet<>();
        evaluators.addAll(internalMap.keySet());
        evaluators.addAll(externalMap.keySet());
        return evaluators.toArray(new String[internalMap.size()]);
    }

    private static void initEvaluators() {
        if (internalMap == null) {
            InternalEvaluatorFactory inf = InternalEvaluatorFactory.getInstance();
            internalMap = new Object2ObjectOpenHashMap<>();

            for (InternalEvaluator eval : inf.getAll()) {
                internalMap.put(eval.getName(), eval);
            }
        }
        if (externalMap == null) {
            externalMap = new Object2ObjectOpenHashMap<>();
            ExternalEvaluatorFactory extf = ExternalEvaluatorFactory.getInstance();
            for (ExternalEvaluator eval : extf.getAll()) {
                externalMap.put(eval.getName(), eval);
            }
        }
    }

}
