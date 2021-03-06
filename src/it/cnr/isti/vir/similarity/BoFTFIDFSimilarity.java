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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.isti.vir.similarity;

import it.cnr.isti.vir.features.AbstractFeaturesCollector;
import it.cnr.isti.vir.features.FeatureClassCollector;
import it.cnr.isti.vir.features.bof.LFWords;
import it.cnr.isti.vir.features.localfeatures.BoFLFGroup;
import it.cnr.isti.vir.file.ArchiveException;
import it.cnr.isti.vir.file.FeaturesCollectorsArchive;
import it.cnr.isti.vir.file.FeaturesCollectorsArchives;
import it.cnr.isti.vir.global.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 *
 * @author Fabrizio
 */
public class BoFTFIDFSimilarity implements ISimilarity<BoFLFGroup>, IRequiresIDF {

    private float[] idf = null;
    private static final long dCount = 0;


	public static final FeatureClassCollector reqFeatures = new FeatureClassCollector(BoFLFGroup.class);
    
    public float[] getIdf() {
		return idf;
	}

	public void setIdf(float[] idf) {
		this.idf = idf;
	}

	public BoFTFIDFSimilarity( Properties properties) throws SimilarityOptionException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, ArchiveException, IOException {
		this();
		
		String archiveForIDFStr = properties.getProperty("BoFTFIDFSimilarity.archiveForIDF");
		if ( archiveForIDFStr != null ) {
			String nWordsStr = properties.getProperty("BoFTFIDFSimilarity.nWords");
			if ( nWordsStr == null ) {
				Log.info("Evaluating IDF on " + archiveForIDFStr + " inferring number of Words");
				idf = LFWords.getIDF_inferringNWords(FeaturesCollectorsArchives.getFromSingleArchive(archiveForIDFStr));
				Log.info("Evaluating IDF " + idf.length + " were found.");
			} else {
				int nWords = Integer.parseInt(nWordsStr);
				Log.info("Evaluating IDF on " + archiveForIDFStr);
				idf = LFWords.getIDF(new FeaturesCollectorsArchive(archiveForIDFStr), nWords);			
			}
		} else {
			Log.info("BoFTFIDFSimilarity was created without TF beacuse BoFTFIDFSimilarity.archiveForIDF was not found in properties.");
		}
		
		
		
		
//		String value = properties.getProperty("useIDF");
//		if ( value != null ) {
//			useIDF = Boolean.parseBoolean(value);
//			System.out.println("UseIDF was set to " + useIDF);
//		}
	}
	
	public BoFTFIDFSimilarity() {
    	this((float[]) null);
    }
    
    public BoFTFIDFSimilarity(float[] idf) {
        super();
        this.idf = idf;
    }

    public long getDistCount() {
        return dCount;
    }

    public FeatureClassCollector getRequestedFeaturesClasses() {
        return reqFeatures;
    }

    public double distance(BoFLFGroup f1, BoFLFGroup f2) {
    	
    	double sim = 0.0;
    	
//		if ( f1.getID().equals(f2.getID())) {
//			System.out.println("FOUND!");
//		}
    	
    	sim = BoFLFGroup.getSimilarity_cosine(f1, f2, idf, false);

    	if ( sim > 1.0 ) {
    		//System.err.println("BoFLFGroup similarity > 1.0 : " + sim);
    		sim = 1.0;
    	}
    	return 1.0 - sim;
    }

    public double distance(BoFLFGroup f1, BoFLFGroup f2, double max) {
        return distance(f1, f2);
    }

    public double distance(AbstractFeaturesCollector f1, AbstractFeaturesCollector f2) {
        return distance((BoFLFGroup) f1.getFeature(BoFLFGroup.class), (BoFLFGroup) f2.getFeature(BoFLFGroup.class));
    }

    public double distance(AbstractFeaturesCollector f1, AbstractFeaturesCollector f2, double max) {
        return distance((BoFLFGroup) f1.getFeature(BoFLFGroup.class), (BoFLFGroup) f2.getFeature(BoFLFGroup.class));
    }

    public String getStatsString() { return ""; };
}

