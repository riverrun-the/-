package Optimizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class Field {

	private int fieldID;			 // フィールドの番号
	private double area;			 // 圃場の持つ面積
	private ArrayList<Period> periodList;	 // 圃場に含まれるPeriodのリスト
	private ArrayList<Period> changedPeriodList;// 動かしたピリオドのリスト
	private double totalAmountOfField;	 // 圃場に含まれているすべてのPeriodの出荷可能量

	// スケジュールを変化させた時に保存しておく値
	private double totalAmountOfFieldPreserved;
// （保存用）圃場に含まれているすべてのPeriodの出荷可能量

	// 暫定最適計画を保存した時に保存しておく値
	private double totalAmountOfFieldBest;
// 暫定最適計画の時の圃場に含まれているすべてのPeriodの出荷可能量

	private Map<Calendar, Double> dayAndShipmentOfField;
// 出荷期間の各日にどれだけ出荷したかを表すマップ



	public Field(int id, double area) {
		this.fieldID = id;
		this.area = area;

		periodList = new ArrayList<Period>();
		changedPeriodList = new ArrayList<Period>();
		dayAndShipmentOfField = new TreeMap<Calendar, Double>();
		resetShipment();
	}


	public boolean addPeriod(Period period){
		// periodがperiodListの後ろに追加される．
		// 早いperiodから順番に重ならないように追加しなければならない．

if(periodList.isEmpty() || periodList.get(periodList.size()-1).getFallowEnd().before(period.getGrowStart())){
			periodList.add(period);
			totalAmountOfField += period.getTotalAmountOfPeriod();
			return true;
		} else {
			return false;	// ピリオドが重なったらエラー
		}
	}


	public void resetShipment(){
		dayAndShipmentOfField.clear();
		for (Period period : periodList) period.resetShipment();
	}


	public void revert() {
		totalAmountOfField = totalAmountOfFieldPreserved;
		for (Period period : changedPeriodList) period.revert();
		resetShipment();
	}


	public void saveAsBest() {
		totalAmountOfFieldBest = totalAmountOfField;
		for (Period period : periodList) period.saveAsBest();
	}


	public void revertBest() {
		totalAmountOfField = totalAmountOfFieldBest;
		for (Period period : periodList) period.revertBest();
		resetShipment();
	}


	public double getShipment(Calendar date, double shippingAmount){
		// ある日（date）にある量（shippingAmount）を出荷するように要求．
		// 実際に出荷できた量（shipment）を返す．

		Period p;
		double shipment;

		p = getShippablePeriod(date);
		if (p != null) {
			shipment = p.getShipment(date, shippingAmount);
			dayAndShipmentOfField.put((Calendar)date.clone(), shipment);
// 出荷日・出荷量を記録
			return shipment;
		}
		return 0.0;
	}


	private Period getShippablePeriod(Calendar date){
		// 出荷可能なピリオドを返す．
		// periodListの中にPeriodのインスタンスが早い順に並んでいることが前提．

		for(Period p:periodList){
			if (p.canShip(date) != null) return p;
		}
		return null;
	}


	public Calendar canShip(Calendar date){
		// その日(date)に出荷可能なPeriodのインスタンスを探す．
// 出荷可能なPeriodのインスタンスがあれば，出荷終了日のクローン（Periodでクローンが作られる）を返す．
		// なければnullを返す．

		Calendar shipEnd;

		for(Period p:periodList){
			if ((shipEnd = p.canShip(date)) != null) return shipEnd;
		}
		return null;
	}


	public double getRestAmountOfField(Calendar date) {
		Period p;

		if ((p = getShippablePeriod(date)) != null) {
			return p.getRestAmountOfPeriod();
		} else {
			return 0.0;
		}

	}


	public double getSurplusOfField(Calendar date) {
		Period p;

		if ((p = getShippablePeriod(date)) != null) {
			return p.getSurplusOfPeriod(date);
		} else {
			return 0.0;
		}

	}


	private void preserve() {
		totalAmountOfFieldPreserved = totalAmountOfField;
	}


	private void calcTotalAmountAndCostOfField(){
		preserve();
		totalAmountOfField = 0.0;
		for(Period p: periodList){
			totalAmountOfField += p.getTotalAmountOfPeriod();
		}
	}


	private boolean checkOverlap(Period p1, Period p2) {
		// オーバーラップのチェック．重なっていたらtrueを返す．
		// p1が時間的にp2よりも早いとする．

		return (p1.getFallowEnd().after(p2.getGrowStart()) || p1.getFallowEnd().equals(p2.getGrowStart()));
	}


	public boolean changePeriodRnd(int days, Calendar dateFrom, int termLength, int numOfTrial) {
		// Periodを動かすのをnumOfTrial回だけトライ．ダメならfalseを返す．

		boolean flag;
		int count;

		// 移動の対象となるPeriodはdateFrom以降のもの
		Calendar d;
		Calendar dateTo;
		int earliestIndex;		// dateFrom以降で最初のPeriodのインデックス
		int latestIndex;		// dateTo以前で最後のPeriodのインデックス

		changedPeriodList.clear();
		dateTo = (Calendar) dateFrom.clone();
		dateTo.add(Calendar.DATE, termLength);

		// dateFrom以降に作付けする、一番早いPeriodを探す
		// periodListには日付の早い方から順番に格納されているという前提
		earliestIndex = -1;
		for(Period period: periodList){
			d = period.getGrowStart();
			if (d.after(dateFrom) || d.equals(dateFrom)) {
				earliestIndex = periodList.indexOf(period);
				//this.earliestIndex = earliestIndex;
				break;
			}
		}

		// dateTo以前に作付けする、一番遅いのPeriodを探す
		// periodListには日付の早い方から順番に格納されているという前提
		latestIndex = -1;
		for(Period period: periodList){
			d = period.getGrowStart();
			if (d.after(dateTo)) {
				break;
			}
			latestIndex = periodList.indexOf(period);
			//this.latestIndex = latestIndex;
		}

		if (earliestIndex == -1 || latestIndex == -1) return false;
// 対象期間内にPeriodが1つもないとエラー

		flag = false;
		count = 0;
		while ((!flag) && count < numOfTrial) {
			int rnd = earliestIndex + (int)(Math.random()*((periodList.size()) - earliestIndex));
			//changedPeriodIndex = rnd;
			flag = changeAllPeriodsOfField(days, rnd, earliestIndex, dateFrom);
			count++;
		}
		if (flag) {
			calcTotalAmountAndCostOfField();
		} else {
			System.out.println("Error! STAY chosen.");
		}

		return flag;
	}


private boolean changeAllPeriodsOfField(int days, int indexOfChangedPeriod, int earliestIndex, Calendar dateFrom){
		// indexOfChangedPeriodのピリオドを動かす．
		// earliestIndexはここで動かす対象のピリオドのうち一番早いピリオド．
//changePeriodRndで求めている．
		// 過去方向（BACK）にピリオドをずらして行く時，earliestIndexで示されるピリオドが
		// dateFromより過去に動いたらエラー

		Direction direction;
		Period aPeriod;	// 着目しているピリオド
		Period periodNext;	// 着目しているピリオドの1つ未来方向（FORTH）に隣のピリオド
		Period periodPrev;	// 着目しているピリオドの1つ過去方向（BACK）に隣のピリオド
		int index;		// ピリオドのインデックス
		int size;		// periodListの全要素数（ピリオドの数）
		Calendar c;
		Calendar prevStartDate;

		size = periodList.size();

		index = indexOfChangedPeriod;

		aPeriod =  periodList.get(index);		// 最初にランダムに動かすperiod
		direction = aPeriod.changeDateRnd(days); // そのperiodを動かす．方向が返る．
		//this.direction = direction;
		changedPeriodList.add(aPeriod);

		switch (direction) {
			case FORTH:
			// 未来方向にずらしていく場合
			if (index >= size - 1) break;
			periodNext = periodList.get(index + 1);
while((index < size - 1) && checkOverlap(aPeriod, periodNext = periodList.get(index + 1))){
			// periodNextを未来方向にずらす．
				c = (Calendar) aPeriod.getFallowEnd().clone();
				c.add(Calendar.DATE, 1);
				periodNext.preserveAndSetPeriod(c);
				changedPeriodList.add(periodNext);

				aPeriod = periodNext;
				index++;
								}
				//System.out.print(".");
			if (testOrderOfPeriods()) {
				System.out.println("Period Order Error!FF");
			}
			break;

			case BACK:
			// 過去方向にずらしていく場合

			//System.out.print("'");

			// indexが0の場合すなわちそれよりも過去にピリオドが存在しない場合
			if (index == 0) {
				// dateFromよりも前に来ないかどうかチェック
				if (aPeriod.getGrowStart().before(dateFrom)) {
					revertChangedPeriods();
					return false;
				}
			break;
			}

				// index > 0 の場合
			periodPrev = periodList.get(index - 1);
while((index > earliestIndex) && checkOverlap(periodPrev = periodList.get(index - 1), aPeriod)){
				// ずらすのはperiodPrev．periodPrevはearliestIndexまで．
				periodPrev.preserve();
				prevStartDate = (Calendar) periodPrev.getGrowStart().clone();
					// overlapをチェックしながら1日ずつ前に動かす．
					while(checkOverlap(periodPrev, aPeriod)) {
						prevStartDate.add(Calendar.DATE, -1);
						periodPrev.setPeriod(prevStartDate);
					}
					changedPeriodList.add(periodPrev);

					aPeriod = periodPrev;
					index--;
				}

				if (aPeriod.getGrowStart().before(dateFrom) || (index > 0 && checkOverlap(periodList.get(index - 1), aPeriod))) {
					revertChangedPeriods();
					return false;
				}

				if (testOrderOfPeriods()) {
					System.out.println("Period Order Error!BB");
					return false;
				}
				break;

			case STAY:
				return false;	// そもそもSTAYにはならないはず
		}
		return true;
	}


	private void revertChangedPeriods() {
		for (Period period : changedPeriodList) period.revert();
		changedPeriodList.clear();
	}


	public int getFieldID() {
		return fieldID;
	}


	public double getArea() {
		return area;
	}


	public ArrayList<Period> getPeriodList() {
		return periodList;
	}


	private Boolean testOrderOfPeriods() {
		Boolean result;
		int i;
		int imax;

		imax = periodList.size() - 1;
		result = false;
		for (i = 0; i < imax; i++) {
			result = result || checkOverlap(periodList.get(i), periodList.get(i + 1));
		}

		return result;
	}


}
