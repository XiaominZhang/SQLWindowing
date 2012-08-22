package com.sap.hadoop.windowing.functions2.window;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.Description;

import com.sap.hadoop.windowing.functions2.annotation.WindowFuncDef;

@WindowFuncDef
(	
		description = @Description(
								name = "denserank", 
								value = "_FUNC_(x) The difference between RANK and DENSE_RANK is that DENSE_RANK leaves no " +
										"gaps in ranking sequence when there are ties. That is, if you were " +
										"ranking a competition using DENSE_RANK and had three people tie for " + 
										"second place, you would say that all three were in second place and " +
										"that the next person came in third."
								),
		supportsWindow = false,
		pivotResult = true
)
public class GenericUDAFDenseRank extends GenericUDAFRank
{
	static final Log LOG = LogFactory.getLog(GenericUDAFDenseRank.class.getName());
	
	protected GenericUDAFRankEvaluator createEvaluator()
	{
		return new GenericUDAFDenseRankEvaluator();
	}
	
	public static class GenericUDAFDenseRankEvaluator extends GenericUDAFRankEvaluator
	{
		/*
		 * Called when the value in the partition has changed. Update the currentRank
		 */
		protected void nextRank(RankBuffer rb)
		{
			rb.currentRank++;
		}
	}
}
