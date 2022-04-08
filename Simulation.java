package Optimizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

import javax.swing.JFrame;


public class Simulation  {

	private Calendar simulationStartDay;	// シミュレーション開始日
	private Calendar simulationEndDay;	// シミュレーション終了日
	private int intervalOfOrder;		// オーダーが来る間隔（日数）
	private double maxNumOfPeriodsPerDay;	// 1日あたりのPeriodの最大数（1年間のPeriodの最大数/365）
	private int termLength;		// 最適化をする範囲の長さ
	private int numOfPeriodsPerTerm;	// termの中にあるピリオドの数の基準値
	private ArrayList<Field> fieldList;

	private Optimizer optimizer;
	private Demand demands;
	private int leadTime;
	TreeMap<Calendar, Double> actualShortageAndSurplusMap;

	JFrame sheduleOutputFrame;

	public Simulation(Optimizer opt, Calendar startDay, Calendar endDay, int interval,Demand demands, int leadTime) {

		optimizer = opt;
		simulationStartDay = startDay;
		simulationStartDay.set(Calendar.MILLISECOND, 0);
		simulationEndDay = endDay;
		simulationEndDay.set(Calendar.MILLISECOND, 0);

		intervalOfOrder = interval;

		this.demands = demands;
		this.leadTime = leadTime;
		maxNumOfPeriodsPerDay = 8.0 / 365;

		termLength = optimizer.getTermLength();
		numOfPeriodsPerTerm = (int) (termLength * maxNumOfPeriodsPerDay) +1;
		fieldList = optimizer.getFields().getFieldList();

		sheduleOutputFrame = new JFrame();
	}


	public void optimizeWithOrders() {
		// とりあえず一定日数ごとにオーダーが来るという設定で考える．

		Calendar today;
		Calendar d;
		Calendar fallowEnd;
		int earliestIndex;
		int numOfPeriodsAfterToday;
		ArrayList<Period> periodList;
		Period additionalPeriod;
		double area;

		for (today = (Calendar) simulationStartDay.clone(); !today.after(simulationEndDay); today.add(Calendar.DATE,
				intervalOfOrder)) {

			// today以降に，常に一定数のPeriodがあるようにする．
			for (Field aField : fieldList) {

				// today以降に作付けする、一番早いのPeriodを探す
				// periodListには日付の早い方から順番に格納されているという前提
				periodList = aField.getPeriodList();
				earliestIndex = periodList.size();
				for (Period period : periodList) {
					d = period.getGrowStart();
					if (d.after(today) || d.equals(today)) {
						earliestIndex = periodList.indexOf(period);
						break;
					}
				}
				numOfPeriodsAfterToday = periodList.size() - earliestIndex;
				area = aField.getArea();
				for (int i = numOfPeriodsAfterToday; i < numOfPeriodsPerTerm; i++) {

					fallowEnd = (Calendar) (periodList.get(periodList.size() - 1).getFallowEnd()).clone();
					fallowEnd.add(Calendar.DATE, 1);
					additionalPeriod = new Period(fallowEnd, area);
					if (!aField.addPeriod(additionalPeriod)) {
						System.out.println("Error in addPeriod");
					}
				}
			}
			// 最適化を実行
			optimizer.optimizeWithOneOrder(today);

			go(fieldList, today, demands.getDemandMap(today, leadTime, termLength), optimizer.getShortageMap(),
					optimizer.getSurplusMap(), simulationStartDay, termLength, demands.getDemandSumInTerm());
		}


	}
	private void go(ArrayList<Field> fieldList2, Calendar today, TreeMap<Calendar, Double> demandMap,
			TreeMap<Calendar, Double> shortageMap, TreeMap<Calendar, Double> surplusMap, Calendar simulationStartDay2,
			int termLength2, double demandSumInTerm) {
		// TODO 自動生成されたメソッド・スタブ

	}


	public double getActualShipmentForTerm(Calendar termStart, Calendar termEnd){
		return optimizer.getActualShipmentForTerm(termStart, termEnd);

	}

}