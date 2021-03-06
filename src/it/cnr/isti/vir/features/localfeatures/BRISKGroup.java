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
package it.cnr.isti.vir.features.localfeatures;

import it.cnr.isti.vir.features.AbstractFeaturesCollector;
import it.cnr.isti.vir.similarity.LocalFeatureMatch;
import it.cnr.isti.vir.similarity.LocalFeaturesMatches;
import it.cnr.isti.vir.util.RandomOperations;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BRISKGroup extends ALocalFeaturesGroup<BRISK> {

	public static final byte version = 0;
	
	public BRISKGroup(BRISK[] arr, AbstractFeaturesCollector fc) {
		super(arr, fc);
	}
	
	public BRISKGroup(BRISK[] arr ) {
		super(arr);
	}

	public BRISKGroup(AbstractFeaturesCollector fc) {
		super(fc);
	}
	
	public BRISKGroup(DataInput in) throws Exception {
		in.readByte(); // version
		int nBytes = in.readInt();
		byte[] bytes = new byte[nBytes];
		in.readFully(bytes);
		ByteBuffer bBuffer = ByteBuffer.wrap(bytes);
		lfArr = new BRISK[bBuffer.getInt()];
		for (int i = 0; i < lfArr.length; i++) {
			lfArr[i] = new BRISK(bBuffer);
			lfArr[i].setLinkedGroup(this);
		}
	}
	
	
	public BRISKGroup(ByteBuffer in ) throws IOException {
		in.get(); 		// version
		in.getInt(); 	// nBytes
		int nLFs = in.getInt();
		lfArr = new BRISK[nLFs];
		for ( int i = 0; i < nLFs; i++ ) {
			lfArr[i] = new BRISK(in);
			lfArr[i].setLinkedGroup(this);
		}
	}

	@Override
	public Class<ORB> getLocalFeatureClass() {
		return ORB.class;
	}

	@Override
	public ALocalFeaturesGroup<BRISK> create(BRISK[] arr, AbstractFeaturesCollector fc) {
		return new BRISKGroup( arr, fc);
	}
	@Override
	public byte getSerVersion() {
		return version;
	}
	
	public BRISKGroup(BufferedReader br, AbstractFeaturesCollector fc) throws IOException {
		super(fc);
		String[] temp = br.readLine().split("(\\s)+");
		
		int n = Integer.parseInt(temp[0]);
		int len = Integer.parseInt(temp[1]);
		
		lfArr = new BRISK[n];
	    for (int i = 0; i < n; i++) {
	    	lfArr[i] = new BRISK(br);
	    	lfArr[i].setLinkedGroup(this);
	    }
	}
	
	static final public double getRangePercMatches(
			ALocalFeaturesGroup<BRISK> sg1,
			ALocalFeaturesGroup<BRISK> sg2,
			int range) {
		
		int count = 0;
		for (BRISK f: sg1.lfArr) {
			if ( hasMatchBelowRadius(f, sg2, range)) {
				count++;
			}
		}
		
		return count / (double) sg1.size();
	}
	
	/**
	 * @param s1	ORB query feature
	 * @param sg	Group of ORB
	 * @param range  Range
	 * @return	true if it exists at least one match below or equal the range
	 */
	static final public boolean hasMatchBelowRadius(BRISK s1, ALocalFeaturesGroup<BRISK> sg, int range) {
		for (BRISK f:sg.lfArr) {
			if ( BRISK.getDistance(s1, f) <= range) {
				return true;
			}
		}
		
		return false;
	}
	

	public static BRISKGroup getRandom() {
		BRISK[] lfArr = new BRISK[RandomOperations.getInt(1001)];
		
		return new BRISKGroup(lfArr);
	}
	
	static final public BRISK getLoweMatch(BRISK s1, ALocalFeaturesGroup<BRISK> sg,
			double conf, int maxFDsq) {
		int distsq1 = Integer.MAX_VALUE;
		int distsq2 = Integer.MAX_VALUE;
		int dsq = 0;
		BRISK curr, best = null;
		BRISK[] arr = sg.lfArr;
		for (int i = 0; i < arr.length; i++) {
			curr = arr[i];
			dsq = BRISK.getDistance(s1, curr);
			if (dsq < 0)
				continue;
			if (dsq < distsq1) {
				distsq2 = distsq1;
				distsq1 = dsq;
				best = curr;
			} else if (dsq < distsq2) {
				distsq2 = dsq;
			}
		}

		// System.out.print(bestSIFT.scale + "\t");
		// if ( bestSIFT.scale > 4 ) return null;
		if (distsq1 > maxFDsq)
			return null;
		if (distsq2 == 0)
			return null;
		/*
		 * Check whether closest distance is less than ratio threshold of
		 * second.
		 */
		if ((double) distsq1 / (double) distsq2 < conf)
			return best;
		return null;
	}

	static final public BRISK getLoweMatch(BRISK s1, ALocalFeaturesGroup<BRISK> sg,
			double conf) {
		int distsq1 = Integer.MAX_VALUE;
		int distsq2 = Integer.MAX_VALUE;
		int dsq = 0;
		BRISK curr, best = null;

		BRISK[] arr = sg.lfArr;
		for (int i = 0; i < arr.length; i++) {
			curr = arr[i];
			dsq = BRISK.getDistance(s1, curr );
			if (dsq < distsq1) {
				distsq2 = distsq1;
				distsq1 = dsq;
				best = curr;
			} else if (dsq < distsq2) {
				distsq2 = dsq;
			}
		}

		if (distsq2 == 0)
			return null;
		/*
		 * Check whether closest distance is less than ratio threshold of
		 * second.
		 */
		if ((double) distsq1 / (double) distsq2 < conf)
			return best;
		return null;
	}
	
	static final public double getLowePercMatches(ALocalFeaturesGroup<BRISK> sg1,
			ALocalFeaturesGroup<BRISK> sg2, double conf) {
		return (double) getLoweNMatches(sg1, sg2, conf) / sg1.size();
	}
	
	static final public int getLoweNMatches(ALocalFeaturesGroup<BRISK> sg1,
			ALocalFeaturesGroup<BRISK> sg2, double conf) {
		if (sg2.size() < 2)
			return 0;
		int nMatches = 0;
		BRISK[] arr = sg1.lfArr;
		for (int i = 0; i < arr.length; i++) {
			if (BRISKGroup.getLoweMatch(arr[i], sg2, conf) != null)
				nMatches++;
		}

		return nMatches;
	}
	
	
	static final public LocalFeaturesMatches getLoweMatches(ALocalFeaturesGroup<BRISK> sg1, ALocalFeaturesGroup<BRISK> sg2, double dRatioThr) {
		LocalFeaturesMatches matches = new LocalFeaturesMatches();
		if ( sg2.size() < 2 ) return null;
		int nMatches = 0;
		BRISK[] arr = sg1.lfArr;
		for (int i=0; i<arr.length; i++ ) {
			BRISK match = BRISKGroup.getLoweMatch(arr[i], sg2, dRatioThr );
			if ( match != null)
				matches.add( new LocalFeatureMatch( arr[i], match ) );
		}
		
		return matches;
	}

	static final public LocalFeaturesMatches getLoweMatches(ALocalFeaturesGroup<BRISK> sg1, ALocalFeaturesGroup<BRISK> sg2, double dRatioThr, final int maxLFDistSq) {
		LocalFeaturesMatches matches = new LocalFeaturesMatches();
		if ( sg2.size() < 2 ) return null;
		int nMatches = 0;
		BRISK[] arr = sg1.lfArr;
		for (int i=0; i<arr.length; i++ ) {
			BRISK match = BRISKGroup.getLoweMatch(arr[i], sg2, dRatioThr, maxLFDistSq );
			if ( match != null)
				matches.add( new LocalFeatureMatch( arr[i], match ) );
		}
		
		return matches;
	}

}
