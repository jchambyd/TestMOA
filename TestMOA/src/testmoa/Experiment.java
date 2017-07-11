/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testmoa;

import moa.core.TimingUtils;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.ArffLoader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import moa.classifiers.meta.*;
import moa.classifiers.core.driftdetection.*;
import moa.classifiers.drift.SingleClassifierDrift;
import moa.streams.ArffFileStream;

public class Experiment {

    public Experiment(){
    }

	public static ArffFileStream getArffDataset(String nameFile) throws FileNotFoundException
	{
		ArffLoader loader = new ArffLoader(new FileReader(nameFile));
		ArffFileStream stream = new ArffFileStream(nameFile, loader.getStructure().numAttributes());				
		return stream;
	}
	
	
	public ArrayList<ClassifierTest> getResult(String name, int numChunks) throws FileNotFoundException
	{
		int numInstances, lengthChunk;
		long evaluateStartTime;
		Instance trainInst;
		//Classifiers
		SingleClassifierDrift learnerEWMA = new SingleClassifierDrift();
		SingleClassifierDrift learnerDDM = new SingleClassifierDrift();
		SingleClassifierDrift learnerEDDM = new SingleClassifierDrift();
		learnerEWMA.driftDetectionMethodOption.setCurrentObject(new EWMAChartDM());
		learnerDDM.driftDetectionMethodOption.setCurrentObject(new DDM());
		learnerEDDM.driftDetectionMethodOption.setCurrentObject(new EDDM());
		
		//Load Dataset
		ArffFileStream stream = getArffDataset(name);
		stream.prepareForUse();
		numInstances = 0;
		//Counting # of instances in the Dataset
		while(stream.hasMoreInstances())
		{
			stream.nextInstance();
			numInstances++;
		}
		stream.restart();

		//Calculate length for each chunck
		lengthChunk = numInstances / numChunks;
		
		//Adjust for use all instances
		/*if( numInstances > (lengthChunk * numChunks))
			lengthChunk++;*/

		ArrayList<ClassifierTest> learners = new ArrayList<>();
		//Selected algorithms
		learners.add(new ClassifierTest(new RCD(), "RCD"));
		learners.add(new ClassifierTest(new WeightedMajorityAlgorithm(), "DWM"));
		learners.add(new ClassifierTest(learnerEWMA, "EWMA"));
		learners.add(new ClassifierTest(learnerDDM, "DDM"));
		learners.add(new ClassifierTest(learnerEDDM, "EDDM"));
		learners.add(new ClassifierTest(new OnlineAccuracyUpdatedEnsemble(), "OAUE"));
		//learners.add(new ClassifierTest(new DoF(), "DoF"));
		
		//Prepare Learners
		for(int i = 0; i < learners.size(); i++)
		{
			//learners.get(i).learner.getOptions().setViaCLIString("-k 4"); 
			learners.get(i).learner.setModelContext(stream.getHeader());
			learners.get(i).learner.prepareForUse();
		}

		int numberSamples = 0;

		for(int i = 0; i < numChunks; i++)
		{
			//Evaluate and train instances by chunck
			for(int j = 0; j < lengthChunk && numberSamples < numInstances; j++)
			{
				trainInst = stream.nextInstance().instance;

				for(int k = 0; k < learners.size(); k++)
				{
					evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

					if (learners.get(k).learner.correctlyClassifies(trainInst))
						learners.get(k).numCorrect++;
					else
						learners.get(k).numIncorrect++;

					learners.get(k).learner.trainOnInstance(trainInst);				
					learners.get(k).time += TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()- evaluateStartTime);				
				}
				numberSamples++;
			}
			//Register results by chunck	
			for(int k = 0; k < learners.size(); k++)
			{
				learners.get(k).accuracies.add(learners.get(k).mxCalculateAccuracy());
				learners.get(k).times.add(learners.get(k).time);
				//Reset values
				learners.get(k).time = 0;
				learners.get(k).numCorrect = 0;
				learners.get(k).numIncorrect = 0;				
			}
		}

		for(int k = 0; k < learners.size(); k++)
		{
			learners.get(k).mxCalculateValues();
		}	
		
		return learners;
	}	
	
    public void run()throws IOException
    {
		//Output File
		PrintWriter outFile = new PrintWriter(new FileWriter("data.txt", false));
		
		//Prepare Datasets
		ArrayList<String> namesDataSet = new ArrayList<>();
		namesDataSet.add("data/iris.arff");
		namesDataSet.add("data/DriftSets/sea.arff");
		
		for(String name : namesDataSet)
		{			
			ArrayList<ClassifierTest> learners = getResult(name, 40);
			
			outFile.println("DATASET: " + name);
			outFile.printf("%12s%12s%12s%11s%11s\n", "Classifier", "Accuracy", "SD-Accu.", "Time", "SD-Time");
			outFile.println("----------------------------------------------------------");
			for(int i = 0; i < learners.size(); i++)
			{
				outFile.printf("%12s %11.2f% 11.2f %11.6f% 11.6f\n", learners.get(i).name, 
																	 learners.get(i).accuracy, 
																	 learners.get(i).sd_accuracy, 
																	 learners.get(i).mean_time, 
																	 learners.get(i).sd_time);
			}
		}
        outFile.close();
    }

    public static void main(String[] args)throws IOException
    {
        Experiment exp = new Experiment();		
        exp.run();
    }
}