package com.sap.hadoop.windowing.functions2.window;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator.AggregationBuffer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils.ObjectInspectorCopyOption;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.IntWritable;

import com.sap.hadoop.windowing.functions2.annotation.WindowFuncDef;
import com.sap.hadoop.windowing.runtime2.RuntimeUtils;

@WindowFuncDef
(	
		description = @Description(
								name = "rank", 
								value = "_FUNC_(x)"
								),
		supportsWindow = false,
		pivotResult = true
)
public class GenericUDAFRank extends AbstractGenericUDAFResolver
{
	static final Log LOG = LogFactory.getLog(GenericUDAFRank.class.getName());
	
	@Override
	public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters) throws SemanticException
	{
		if (parameters.length < 1)
		{
			throw new UDFArgumentTypeException(parameters.length - 1, "One or more arguments are expected.");
		}
		for(int i=0; i<parameters.length; i++)
		{
			ObjectInspector oi = TypeInfoUtils.getStandardJavaObjectInspectorFromTypeInfo(parameters[i]);
			if (!ObjectInspectorUtils.compareSupported(oi))
			{
				throw new UDFArgumentTypeException(i, 
					"Cannot support comparison of map<> type or complex type containing map<>.");
			}
		}
		return createEvaluator();
	}
	
	protected GenericUDAFRankEvaluator createEvaluator()
	{
		return new GenericUDAFRankEvaluator();
	}

	static class RankBuffer implements AggregationBuffer
	{
		ArrayList<IntWritable> rowNums;
		int currentRowNum;
		Object[] currVal;
		int currentRank;
		int numParams;
		
		RankBuffer(int numParams)
		{
			this.numParams = numParams;
			init();
		}

		void init()
		{
			rowNums = new ArrayList<IntWritable>();
			currentRowNum = 0;
			currentRank = 0;
			currVal = new Object[numParams];
		}

		void incrRowNum() { currentRowNum++; }

		void addRank()
		{
			rowNums.add(new IntWritable(currentRank));
		}
	}
	
	public static class GenericUDAFRankEvaluator extends GenericUDAFEvaluator
	{
		ObjectInspector[] inputOI;
		ObjectInspector[] outputOI;
		
		@Override
		public ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException
		{
			super.init(m, parameters);
			if (m != Mode.COMPLETE)
			{
				throw new HiveException(
						"Only COMPLETE mode supported for Rank function");
			}
			inputOI = parameters;
			outputOI = new ObjectInspector[inputOI.length];
			for(int i=0; i < inputOI.length; i++)
			{
				outputOI[i] = ObjectInspectorUtils.getStandardObjectInspector(inputOI[i], ObjectInspectorCopyOption.JAVA);
			}
			return ObjectInspectorFactory.getStandardListObjectInspector(PrimitiveObjectInspectorFactory.writableIntObjectInspector);
		}

		@Override
		public AggregationBuffer getNewAggregationBuffer() throws HiveException
		{
			return new RankBuffer(inputOI.length);
		}

		@Override
		public void reset(AggregationBuffer agg) throws HiveException
		{
			((RankBuffer) agg).init();
		}

		@Override
		public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException
		{
			RankBuffer rb = (RankBuffer) agg;
			 int c = RuntimeUtils.compare(rb.currVal, outputOI, parameters, inputOI);
			 rb.incrRowNum();
			if ( rb.currentRowNum == 1 || c != 0 )
			{
				nextRank(rb);
				rb.currVal = RuntimeUtils.copyToStandardObject(parameters, inputOI, ObjectInspectorCopyOption.JAVA);
			}
			rb.addRank();
		}
		
		/*
		 * Called when the value in the partition has changed. Update the currentRank
		 */
		protected void nextRank(RankBuffer rb)
		{
			rb.currentRank = rb.currentRowNum;
		}

		@Override
		public Object terminatePartial(AggregationBuffer agg) throws HiveException
		{
			throw new HiveException("terminatePartial not supported");
		}

		@Override
		public void merge(AggregationBuffer agg, Object partial) throws HiveException
		{
			throw new HiveException("merge not supported");
		}

		@Override
		public Object terminate(AggregationBuffer agg) throws HiveException
		{
			return ((RankBuffer) agg).rowNums;
		}

	}


}
