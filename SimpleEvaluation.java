package Optimizer;

public class SimpleEvaluation implements Evaluation {
	
	public double eval(double shortageAndSurplus) {
		return Math.abs(shortageAndSurplus);
	}
}
