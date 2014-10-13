package it.cnr.isti.vir.similarity.metric;

import it.cnr.isti.vir.clustering.IMeanEvaluator;
import it.cnr.isti.vir.distance.L1;
import it.cnr.isti.vir.features.AbstractFeaturesCollector;
import it.cnr.isti.vir.features.FeatureClassCollector;
import it.cnr.isti.vir.features.localfeatures.FloatsLF;

import java.util.Collection;
import java.util.Properties;

public class FloatsLFL1Metric  implements IMetric<FloatsLF>, IMeanEvaluator<FloatsLF> {

	private static long distCount = 0;
	private static final FeatureClassCollector reqFeatures = new FeatureClassCollector(FloatsLF.class);
	
	public final long getDistCount() {
		return distCount;
	}
	
	public FloatsLFL1Metric(Properties properties) {
		
	}
	
	public FloatsLFL1Metric() {
		
	}
	
	@Override
	public FeatureClassCollector getRequestedFeaturesClasses() {		
		return reqFeatures;
	}
	
	public String toString() {
		return this.getClass().toString();
	}

	
	@Override
	public final double distance(AbstractFeaturesCollector f1, AbstractFeaturesCollector f2 ) {
		return distance( f1.getFeature(FloatsLF.class), f2.getFeature(FloatsLF.class));
	}
	
	@Override
	public final double distance(AbstractFeaturesCollector f1, AbstractFeaturesCollector f2, double max ) {
		return distance( f1.getFeature(FloatsLF.class), f2.getFeature(FloatsLF.class), max);
	}
	
	@Override
	public final double distance(FloatsLF f1, FloatsLF f2) {
		return L1.get(f1.getValues(), f2.getValues());	
	}
	
	@Override
	public final double distance(FloatsLF f1, FloatsLF f2, double max) {
		// TODO 
		return L1.get(f1.getValues(), f2.getValues() );
	}

	@Override
	public FloatsLF getMean(Collection<FloatsLF> coll) {
		return FloatsLF.getMean(coll);
	}
	
}
