package testmoa;


import java.util.ArrayList;
import moa.classifiers.Classifier;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author JorgeCristhian
 */
public class ClassifierTest {
	public Classifier learner;
	public String name;
	public int numCorrect;
	public int numIncorrect;
	public ArrayList<Double> accuracies;
	public ArrayList<Double> times;
	public double time;
	public double accuracy;
	public double sd_accuracy;
	public double mean_time;
	public double sd_time;
	
	public ClassifierTest(Classifier learner, String name)
	{
		this.learner = learner;
		this.name = name;
		this.accuracies = new ArrayList<>();
		this.times = new ArrayList<>();
	}	
	
	public double mxCalculateAccuracy()
	{
		return 100.0 * (double) this.numCorrect / (double) (this.numCorrect + this.numIncorrect);
	}
	
	public void mxCalculateValues()
	{
		double sum_accuracies = 0, sum_times = 0;
		for(int i = 0; i < this.accuracies.size(); i++)
		{
			sum_accuracies += this.accuracies.get(i);
			sum_times += this.times.get(i);
		}
		
		this.accuracy = sum_accuracies / this.accuracies.size();
		this.sd_accuracy = this.mxCalculateStandardDeviation(this.accuracies, this.accuracy);
		this.mean_time = sum_times / this.times.size();
		this.sd_time = this.mxCalculateStandardDeviation(this.times, this.mean_time);		
	}
	
	public double mxCalculateStandardDeviation(ArrayList<Double> loData, double mean)
    {        
        double sum = 0;
        
        for(int i = 0; i < loData.size(); i++)
            sum += Math.pow(loData.get(i) - mean, 2.0);
        
        return Math.sqrt(sum / loData.size());
    }	
}
