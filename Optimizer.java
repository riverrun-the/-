package Optimizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

public class Optimizer {

	private Demand demands;
	private Fields fields;
	private int termLength;				// 最適化をする範囲の長さ
	private int leadTime;					// オーダーの情報リードタイム
	private double initialT;				// 初期温度
	private double ratio;					// 温度を更新する比

	private Evaluation evaluation;				// 評価関数のための計算方法
	private TreeMap<Calendar, Double> shortageMap;
// termStartからtermLength（日）までの期間の日毎の不足量のMap（評価関数用）
	private TreeMap<Calendar, Double> surplusMap;
// termStartからtermLength（日）までの期間の日毎の生産過剰量のMap（評価関数用）
	private TreeMap<Calendar, Double> actualShortageMap;
// termStartからtermLength（日）までの期間の日毎の過不足量のMap（最終計画用）
	private TreeMap<Calendar, Double> actualSurplusMap;
// termStartからtermLength（日）までの期間の日毎の過不足量のMap（最終計画用）

	private int rangeOfChangeDate;		// 乱数でPeriodを動かす日数の上限
	private int maxNumOfChangedFields;	// 乱数で動かすFieldの数の上限
	private int numOfTrial;		// あるField内のPeriodを動かす時の試行回数の上限

	// 記録関係
	private ArrayList<Double> evalValueList;
// 評価関数値，焼きなましの試行ごとの値をすべて記録
	private ArrayList<Double> bestEvalValueListInOneOpt;
// 最良な評価関数値，焼きなましの試行ごとの値をすべて記録
	private ArrayList<Double> totalShortageList;
// 生産不足量（負），焼きなましの試行ごとの値をすべて記録
	private ArrayList<Double> totalSurplusList;
// 生産過剰量（正），焼きなましの試行ごとの値をすべて記録
	private ArrayList<Double> transitionProbabilityList;
// 評価関数値，焼きなましの試行ごとの値をすべて記録

	JFrame sheduleOutputFrame;


	// Builderクラスを使用してコンストラクタの引数を減らす．
	public static class Builder {
		private Fields fields;
		private Demand demands;
		private Evaluation evaluation;

		private int termLength = 365;
		private int leadTime = 30;
		private double initialT = 1.0E+7;
		private double ratio = 0.95;
		private int rangeOfChangeDate = 5;
		private int maxNumOfChangedFields = 20;
		private int numOfTrial = 10;


		public Builder(Fields fields, Demand demands, Evaluation evaluation, Calendar simulationStartDay) {
			this.fields = fields;
			this.demands = demands;
			this.evaluation = evaluation;
		}

		public Builder termLength(int termLength) {
			this.termLength = termLength;
			return this;
		}

		public Builder leadTime(int leadTime) {
			this.leadTime = leadTime;
			return this;
		}

		public Builder t(double initialT) {
			this.initialT = initialT;
			return this;
		}

		public Builder ratio(double ratio) {
			this.ratio = ratio;
			return this;
		}

		public Builder rangeOfChangeDate(int rangeOfChangeDate) {
			this.rangeOfChangeDate = rangeOfChangeDate;
			return this;
		}

		public Builder maxNumOfChangedFields(int maxNumOfChangedFields) {
			this.maxNumOfChangedFields = maxNumOfChangedFields;
			return this;
		}

		public Builder numOfTrial(int numOfTrial) {
			this.numOfTrial = numOfTrial;
			return this;
		}

		public Optimizer build() {
			return new Optimizer(this);
		}
	}

	private Optimizer(Builder builder) {
		this.fields = builder.fields;
		this.demands = builder.demands;
		this.termLength = builder.termLength;
		this.leadTime = builder.leadTime;

		this.initialT = builder.initialT;
		this.ratio = builder.ratio;

		this.evaluation = builder.evaluation;
		this.rangeOfChangeDate = builder.rangeOfChangeDate;
		this.maxNumOfChangedFields = builder.maxNumOfChangedFields;
		this.numOfTrial = builder.numOfTrial;

		shortageMap = new TreeMap<Calendar, Double>();
		surplusMap = new TreeMap<Calendar, Double>();
		actualShortageMap = new TreeMap<Calendar, Double>();
		actualSurplusMap = new TreeMap<Calendar, Double>();

		evalValueList = new ArrayList<Double>();
		bestEvalValueListInOneOpt = new ArrayList<Double>();
		totalShortageList = new ArrayList<Double>();
		totalSurplusList = new ArrayList<Double>();
		transitionProbabilityList = new ArrayList<Double>();

		sheduleOutputFrame = new JFrame();
	}


	private double getShipmentForTerm(Calendar termStart) {
		// termStartからtermLength（日）までの期間の出荷を行う．
		// 情報リードタイムをleadTime（日）とする．
		// 期間の出荷全体に対する評価関数（evaluation）の値（evalValue）を返す．

		Calendar shipDate;
		Calendar termEnd;
		double demand;
		double evalValue;
		double shortage;
		double surplus;

		termEnd = (Calendar) termStart.clone();
		termEnd.add(Calendar.DATE, termLength);

		evalValue = 0;
		shortageMap.clear();
		fields.resetShipment();
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			demand = demands.getDemand(termStart, leadTime, shipDate);
			shortage = fields.getShipment(shipDate, demand);
			shortageMap.put((Calendar)shipDate.clone(), shortage);
		}

		surplusMap.clear();
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			surplus = fields.getSurplus(shipDate);
			surplusMap.put((Calendar)shipDate.clone(), surplus);
		}

		evalValue = 0.0;
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			shortage = shortageMap.get(shipDate);
			surplus = surplusMap.get(shipDate);
			evalValue += evaluation.eval(shortage + surplus);
		}
		return evalValue;
	}


	public double getActualShipmentForTerm(Calendar termStart, Calendar termEnd) {
		// termStartからtermEndまで実需に応じた出荷を行う．
		// 戻り値は日付とそれに対応した実需に対する生産過剰量（正），不足量（負）のTreeMap．

		Calendar shipDate;
		double demand;
		double evalValue;
		double shortage;
		double surplus;


		actualShortageMap.clear();
		fields.resetShipment();
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			demand = demands.getThisYearsDemand(shipDate);
			shortage = fields.getShipment(shipDate, demand);
			actualShortageMap.put((Calendar)shipDate.clone(), shortage);
		}

		for(Calendar cal:actualShortageMap.keySet()){
			System.out.println(cal.getTime()+" "+actualShortageMap.get(cal));
		}

		actualSurplusMap.clear();
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			surplus = fields.getSurplus(shipDate);
			actualSurplusMap.put((Calendar)shipDate.clone(), surplus);
		}

		for(Calendar cal:actualSurplusMap.keySet()){
			System.out.println(cal.getTime()+" "+actualSurplusMap.get(cal));
		}

		evalValue = 0.0;
		for (shipDate = (Calendar) termStart.clone(); !shipDate.after(termEnd); shipDate.add(Calendar.DATE, 1)) {
			shortage = actualShortageMap.get(shipDate);
			surplus = actualSurplusMap.get(shipDate);
			evalValue += evaluation.eval(shortage + surplus);
		}
		return evalValue;
	}


	public void optimizeWithOneOrder(Calendar today ) {
		// 焼きなまし法を実行．
		// 栽培計画を変更して，評価関数値を比較して，評価関数値が最小の計画を探す．
		// このメソッドを終わるときには，Fields以下には最適解が設定されている．

		double evalValuePresent;		// 現時点の評価関数値
		double evalValueCandidate;		// 新しく考える栽培計画の評価関数値
		double evalValueBest;			// 現時点までの最小の評価関数値
		double delta;	// 新しく考える栽培計画の評価関数値 - 現時点の評価関数値
		double t;				// 温度（変化する）
		double DELTA;

		totalShortageList.clear();// 焼きなましの試行ごとの生産不足量（負）をリセット．
		totalSurplusList.clear();// 焼きなましの試行ごとの生産過剰量（正）をリセット．　

		evalValuePresent = getShipmentForTerm(today);
		evalValueBest = evalValuePresent;
		fields.saveAsBest();
		t = initialT;
		long start = System.nanoTime();	//時間計測スタート
		while(t > 1.0) {
			//System.out.println("Call Fields:getRandomNeighbor");

			// 焼きなましのために計画をランダムに変更．
			fields.getRandomNeighbor(rangeOfChangeDate, today, termLength, maxNumOfChangedFields, numOfTrial);

			// 暫定計画で出荷を行い，評価関数を計算．
			evalValueCandidate = getShipmentForTerm(today);

			delta = evalValueCandidate - evalValuePresent;

			if (delta <= 0) {
				if (evalValueCandidate < evalValueBest) {
					evalValueBest = evalValueCandidate;
					fields.saveAsBest();
				}
				evalValuePresent = evalValueCandidate;
				updateTotalShortageAndSurplus();
			} else {
				// 遷移確率
				DELTA = Math.exp(-(delta *1E+46/ t));
				transitionProbabilityList.add(DELTA);
				if (DELTA > Math.random()) {

					evalValuePresent = evalValueCandidate;
// 改悪解をとる場合
					updateTotalShortageAndSurplus();
				} else {

// 解を更新しないのでtotalShortageListとtotalSurplusListは前の値を再度入れる．			//totalShortageList.add(totalShortageList.get(totalShortageList.size() - 1));			//totalSurplusList.add(totalSurplusList.get(totalSurplusList.size() - 1));
					updateTotalShortageAndSurplus();//追加
				fields.revert();		// 改悪解をとらない場合
				}
			}
			evalValueList.add(evalValuePresent);
			bestEvalValueListInOneOpt.add(evalValueBest);

			//go(fields.getFieldList(), today, demands.getDemandMap(today,leadTime, termLength), shortageMap, surplusMap, simulationStartDay, termLength, demands.getDemandSumInTerm());
		t = t * ratio;		// 温度の更新
		}

		long nanoSeconds = System.nanoTime() - start;
		long totalSeconds = TimeUnit.NANOSECONDS.toSeconds(nanoSeconds);
		int hours = (int)(totalSeconds / 3600);
		int minutes = (int)((totalSeconds % 3600) / 60);
		int seconds = (int)(totalSeconds % 60);
		System.out.printf("処理時間：%d時間%d分%d秒　（総秒数：%d秒）\n", hours, minutes, seconds, totalSeconds);
		System.out.println(today.getTime()+"@@ "+evalValueBest);
		fields.revertBest();	// 最適解に設定

	}


	private void updateTotalShortageAndSurplus() {
		// shortageAndSurplusMapを集計．
		// 正ならば，totalSurplusListに追加
		// 負ならば，totalShortageListに追加

		double sumShortage;
		double sumSurplus;

		sumShortage = 0;
		sumSurplus = 0;

		for (double s : shortageMap.values()) {
			sumShortage += s;
		}
		for (double s : surplusMap.values()) {
			sumSurplus += s;
		}

		totalShortageList.add(sumShortage);
		totalSurplusList.add(sumSurplus);
	}



	public int getTermLength() {
		return termLength;
	}

	public Fields getFields() {
		return fields;
	}

	public TreeMap<Calendar, Double> getShortageMap(){
		return shortageMap;
	}

	public TreeMap<Calendar, Double> getSurplusMap(){
		return surplusMap;
	}


}