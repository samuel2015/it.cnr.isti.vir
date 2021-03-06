/*******************************************************************************
 * Copyright (c) 2013, Fabrizio Falchi (NeMIS Lab., ISTI-CNR, Italy)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package it.cnr.isti.vir.similarity.knn;

import it.cnr.isti.vir.features.AbstractFeature;
import it.cnr.isti.vir.features.AbstractFeaturesCollector;
import it.cnr.isti.vir.global.Log;
import it.cnr.isti.vir.global.ParallelOptions;
import it.cnr.isti.vir.id.IHasID;
import it.cnr.isti.vir.similarity.ISimilarity;
import it.cnr.isti.vir.similarity.pqueues.AbstractSimPQueue;
import it.cnr.isti.vir.similarity.results.ISimilarityResults;
import it.cnr.isti.vir.util.SplitInGroups;
import it.cnr.isti.vir.util.TimeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class KNNPQueue<F> {
	
	static boolean parallel = true;

	protected final AbstractSimPQueue pQueue;
	protected final ISimilarity sim;
	public final F query;
	protected final boolean storeID;
	public double excDistance = Double.MAX_VALUE;
	
	public KNNPQueue(AbstractSimPQueue pQueue, ISimilarity sim, F query) {
		this(pQueue,sim,query,false);
	}
	
	public KNNPQueue(AbstractSimPQueue pQueue, ISimilarity sim, F query, boolean storeID) {
		this.pQueue = pQueue;
		this.sim = sim;
		this.query = query;
		this.storeID = storeID;
	}
	
	public final boolean offer(F obj) {
		double tDistance;
		if ( query instanceof AbstractFeaturesCollector )
			tDistance = sim.distance((AbstractFeaturesCollector) query, (AbstractFeaturesCollector)  obj,pQueue.excDistance);
		else 
			tDistance = sim.distance((AbstractFeature) query, (AbstractFeature)  obj,pQueue.excDistance);
		if ( tDistance >=0 && tDistance <= pQueue.excDistance )	{
			offer(obj, tDistance);
		}
		return false;
	}
	
	public final synchronized void offer(F obj, double distance) {
		if ( distance <= pQueue.excDistance  ) {
			if ( storeID ) pQueue.offer( ((IHasID) obj).getID(),distance);
			else pQueue.offer(obj, distance);
			excDistance = pQueue.excDistance;
		}		
	}
	
	public final void offerAll(Collection<F> coll) throws InterruptedException {
		if ( ArrayList.class.isInstance(coll)) {
			offerAll( (ArrayList) coll);
			return;
		}
		
		final ArrayList<F> arrColl = new ArrayList(coll.size());
		for (Iterator<F> it = coll.iterator(); it.hasNext(); ) {
			arrColl.add(it.next());
		}
		
		offerAll(arrColl);
	}
	
	public final void offerAll_seq(ArrayList<F> coll) {

		for (int iO = 0; iO<coll.size(); iO++) {
			offer(coll.get(iO));
		}

	}
	
	static class OfferAllArray implements Runnable {
        private final int from;
        private final int to;
        private final Object[] arrColl;
        private final KNNPQueue knn;
        OfferAllArray(KNNPQueue knn, int from, int to, Object[] arrColl) {
            this.from = from;
            this.to = to;
            this.arrColl = arrColl;
            this.knn = knn;
        }
        
        @Override
        public void run() {
           
       		TimeManager tM = new TimeManager();
               for (int i=from; i<=to; i++) {
               	if ( tM.hasToOutput() ) {
               		Log.info_verbose("\t"+"KNNPQueue OfferAll thread [" + from + "," + to + "] at " + (double)(i-from)/(to-from)*100.0 + "%");
               	}
               	knn.offer( arrColl[i] );
            }

        }                
    }
	
	
	static class OfferAllArrayList implements Runnable {
        private final int from;
        private final int to;
        private final ArrayList coll;
        private final KNNPQueue knn;
        
        OfferAllArrayList(KNNPQueue knn, int from, int to, ArrayList coll ) {
            this.from = from;
            this.to = to;
            this.coll = coll;
            this.knn = knn;
        }
        
        @Override
        public void run() {

       		TimeManager tM = new TimeManager();
            for (int i=from; i<=to; i++) {
            	if ( tM.hasToOutput() ) {
          			Log.info_verbose("\t"+"KNNPQueue OfferAll thread [" + from + "," + to + "] at " + (double)(i-from)/(to-from)*100.0 + "%");
          		}
               	knn.offer( coll.get(i) );
            }
        	
        }                
    }
	
	public final void offerAll(F[] coll) throws InterruptedException {
		
		if ( parallel ) {
			
			int threadN = ParallelOptions.reserveNFreeProcessors() +1;
	        Thread[] thread = new Thread[threadN];
	        int[] group = SplitInGroups.split(coll.length, thread.length);
	        int from=0;
	        for ( int i=0; i<group.length; i++ ) {
	        	int curr=group[i];
	        	if ( curr == 0 ) break;
	        	int to=from+curr-1;
	        	thread[i] = new Thread( new OfferAllArray(this, from,to,coll) ) ;
	        	thread[i].start();
	        	from=to+1;
	        }
	        
	        for ( Thread t : thread ) {
        		if ( t != null ) t.join();
	        }
	        ParallelOptions.free(threadN-1);
		}
		else
		{
			for (int i = 0; i < coll.length; i++) {
				offer( coll[i]);
			}
		}
	}
		
	public final void offerAll(ArrayList<F> coll) throws InterruptedException {
		
		if ( parallel ) {
			int threadN = ParallelOptions.reserveNFreeProcessors() +1;
	        Thread[] thread = new Thread[threadN];
	        int[] group = SplitInGroups.split(coll.size(), thread.length);
	        int from=0;
	        for ( int i=0; i<group.length; i++ ) {
	        	int curr=group[i];
	        	if ( curr == 0 ) break;
	        	int to=from+curr-1;
	        	thread[i] = new Thread( new OfferAllArrayList(this, from,to,coll) ) ;
	        	thread[i].start();
	        	from=to+1;
	        }
	        
	        for ( Thread t : thread ) {
        		if ( t != null ) t.join();
	        }
			ParallelOptions.free(threadN-1);
			
		}
		else
		{
			for (int i = 0; i < coll.size(); i++) {
				offer( coll.get(i));
			}
		}
	}
	
	public final F getFirstObject() {
		return (F) pQueue.getFirstObject();
	}
	
	public final ISimilarityResults getResults() {
		ISimilarityResults res = pQueue.getResults();
		res.setQuery(query);
		return res;
	}
	
	public final ISimilarityResults getResultsIDs() {
		return getResults().getResultsIDs();
	}
	
	public String toString() {
		return getResults().toString();
	}
	
	public boolean equals(Object obj) {
		if ( this == obj) return true;
		KNNPQueue that = (KNNPQueue) obj;
		if ( this.query != that.query ) return false;
		if ( ! this.getResults().equalResults(that.getResults()) ) return false;
		return true;
	}

	
	public double getLastDist() {
		return pQueue.getLastDist();
	}
	
	public int hashCode() {
		  assert false : "hashCode not designed";
		  return 42; // any arbitrary constant will do 
		  }

}
