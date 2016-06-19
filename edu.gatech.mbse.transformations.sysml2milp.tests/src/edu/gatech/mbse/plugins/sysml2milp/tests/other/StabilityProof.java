/**
 * Copyright (c) 2015, Model-Based Systems Engineering Center, Georgia Institute of Technology.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.gatech.mbse.plugins.sysml2milp.tests.other;

import java.math.BigDecimal;
import java.math.MathContext;

public class StabilityProof {

	/**
	 * Demonstrates stability condition.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		double increment = 0.05;
		double time = -increment;
		double throughput = 22.05;
		double productionTime = 34.2;
		
		int wip = 0;
		int maxWIP = 0;
		
		// Ensure termination of "simulation" after finite time
		while (wip < 10 && time < 100000.0) {
			time += increment;
			
			// Avoid rounding errors - above comparisons are very sensitive!
			time = (new BigDecimal(time).round(new MathContext(("" + time).indexOf(".") + 3)).doubleValue());
			
			if (time % productionTime == 0 && wip > 0)
				wip--;
			
			if (time % throughput == 0)
				wip++;
			
			if (wip > maxWIP)
				maxWIP = wip;
		
			if (time % 50 == 0)
				System.out.println("State at t=" + time + "s: WIP = " + wip);
			
			//System.out.println("State at t=" + time + "s: WIP = " + wip);
			//try {
			//	Thread.sleep(1);
			//} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
		}
		
		// With the given parameters, max WIP was 2 -> as expected from concept
		System.out.println("TERMINATED - MAX WIP WAS: " + maxWIP);
	}

}
