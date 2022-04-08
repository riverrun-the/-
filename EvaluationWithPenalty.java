package Optimizer;

public class EvaluationWithPenalty implements Evaluation {
	
	double penalty;
	
	public EvaluationWithPenalty(double penalty) {
		this.penalty = penalty;
	}
	
	public double eval(double shortageAndSurplus) {
		if (shortageAndSurplus < 0) {
			return Math.abs(shortageAndSurplus * penalty);
		} else {
			return Math.abs(shortageAndSurplus);
		}
	}
}
