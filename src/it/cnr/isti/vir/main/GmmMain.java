/*******************************************************************************
 * Copyright (c), Fabrizio Falchi and Lucia Vadicamo (NeMIS Lab., ISTI-CNR, Italy)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package it.cnr.isti.vir.main;

import it.cnr.isti.vir.experiments.Launch;
import it.cnr.isti.vir.features.AbstractFeature;
import it.cnr.isti.vir.features.AbstractFeaturesCollector;
import it.cnr.isti.vir.features.Gmm;
import it.cnr.isti.vir.features.bof.LFWords;
import it.cnr.isti.vir.features.localfeatures.ALocalFeature;
import it.cnr.isti.vir.features.localfeatures.ALocalFeaturesGroup;
import it.cnr.isti.vir.file.FeaturesCollectorsArchive;
import it.cnr.isti.vir.global.Log;
import it.cnr.isti.vir.util.PropertiesUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

public class GmmMain {

	public static final String className = "GmmMain";
	public static void usage() {
		System.out.println("Usage: " + className + "<properties filename>.properties");
		System.out.println();
		System.out.println("Properties file must contain:");
		System.out.println("- "+className+".inArchive=<archive file name>");
		System.out.println("- "+className+".k=<number of Gaussians>");
		System.out.println("- "+className+".outFileName=<file name>");
		System.out.println("- "+className+".featureClass=<class of the feature>");
		System.out.println("- "+className+".centroidsFileName=<File Name>");
		System.out.println("Properties file optionals:");
		System.out.println("- ["+className+".minDistortionFileName=<File Name>]");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {
		if ( args.length != 1) {
			usage();
		} else {
			Launch.launch(GmmMain.class.getName(), args[0]);
		}
	}

	public static void launch(Properties prop) throws Exception {
		// Input Archive (learning archive)
		File inFile  = PropertiesUtils.getFile(prop,className+".inArchive");
		FeaturesCollectorsArchive inArchive = new FeaturesCollectorsArchive( inFile );
		//number of Gaussian
		String outAbsolutePath = PropertiesUtils.getAbsolutePath(prop, className+".outFileName");
		//centroids file name
		File centroidsFile  = PropertiesUtils.getFile(prop, className+".centroidsFileName");
		int k = PropertiesUtils.getInt(prop,className+ ".k");

		// Features or Local Features Group class
		Class c = PropertiesUtils.getClass(prop, className+".featureClass");

		File minDistortionFile = PropertiesUtils.getFile_orNull(prop, className+".minDistortionOutFileName");

		Log.info("GMM computation, number of Gaussian " + k);
		Log.info("Learning Archive:"+ inArchive.getInfo());

		LFWords<AbstractFeature> centroid = new LFWords(centroidsFile);
		double minDistSqr=0.0;

		if(minDistortionFile !=null) {
			BufferedInputStream bf = new BufferedInputStream(new FileInputStream(minDistortionFile));
			// read min distortion
			byte[] data = new byte[k];
			bf.read(data);
			ByteBuffer buffer = ByteBuffer.wrap(data);
			minDistSqr  = buffer.getDouble();
			bf.close();
		}
		else {
			if ( ALocalFeature.class.isAssignableFrom(c)  ) {
				Class<? extends ALocalFeaturesGroup> cGroup = ALocalFeaturesGroup.getGroupClass( c );
				Log.info("Group class has been set to: " + cGroup);
				int nLF_archive = inArchive.getNumberOfLocalFeatures(cGroup);			
				int counter = 0;
				for ( AbstractFeaturesCollector fc : inArchive ) {
					ALocalFeaturesGroup lfGroup = fc.getFeature(cGroup);
					if ( lfGroup == null || lfGroup.size() == 0 ) continue;
					ALocalFeature[] lfArr = lfGroup.lfArr;
					for(ALocalFeature lf:lfArr) {
						double dist=centroid.getNNDistance(lf);
						minDistSqr+=dist*dist;
						counter++;	
					}
				}
				minDistSqr/=counter;
				Log.info("Set Min Distortion (squared): " +minDistSqr );
			} else {			
				int counter = 0;
				for ( AbstractFeaturesCollector fc : inArchive ) {
					AbstractFeature af = fc.getFeature(c);
					if ( af == null ) continue;
					double dist=centroid.getNNDistance(af);
					minDistSqr+=dist*dist;
					counter++;
				}
				minDistSqr/=counter;
				Log.info("Set Min Distortion (squared): " +minDistSqr );
			}

		}

		Gmm gmm = new Gmm(inArchive, centroid, (float) minDistSqr);
		// write gmmFileName
		gmm.writeData(outAbsolutePath);
		
		inArchive.close();
	}
}	
