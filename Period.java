package Optimizer;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class Period {

	// データの格納されたstatic変数
	private static ShipDays shipDays;	// 栽培にかかる日数と出荷期間（日ごとに違う）
	private static Yield yield;	// 収量 [kg/10a]（日ごとに違う），廃棄 [kg/10a]（日ごとに違う）
	private double fieldArea;	// このインスタンスが属する圃場の面積

	// Periodのインスタンスが持つスケジュール
	private Calendar growStart;			// 栽培開始日
	private Calendar shipStart; 			// 出荷開始日
	private Calendar shipEnd;			// 出荷終了日
	private int fallowPeriod;			// このピリオドの後に置く休耕期間
	private Calendar fallowEnd;			// 後に置く休耕期間も含めたピリオドの終わり

	// 変更する候補としてのスケジュール
	private Calendar growStartPreserved;	// 動かす前の栽培開始日
	private Calendar shipStartPreserved;	// 動かす前の出荷開始日
	private Calendar shipEndPreserved;	// 動かす前の出荷終了日
	private int fallowPeriodPreserved;	// 動かす前のピリオドの後に置く休耕期間
	private Calendar fallowEndPreserved;	// 動かす前の（ピリオドの後に置く休耕期間も含めた）ピリオドの終わり

	// 焼きなまし時の暫定最適計画の時のスケジュール
	private Calendar growStartBest;			// 最適計画の時の栽培開始日
	private Calendar shipStartBest;			// 最適計画の時の出荷開始日
	private Calendar shipEndBest;			// 最適計画の時の出荷終了日
	private int fallowPeriodBest;			// 最適計画の時のピリオドの後に置く休耕期間
	private Calendar fallowEndBest;			// 最適計画の時の（ピリオドの後に置く休耕期間も含めた）ピリオドの終わり

	// 確定したスケジュールから決まる集計データ
	// スケジュールが変わるとリセットされる．
	private double totalAmount;								// 最大出荷可能量
	// スケジュールが変わるとリセットされ，出荷に応じて変化する．
	private double shippedAmount;		// 現時点での出荷した総量
	private double restAmount;		// まだ出荷されていない量 totalAmount-shippedAmount
	private Map<Calendar, Double> dayAndShipmentOfPeriod;
// 出荷期間の各日にどれだけ出荷したかを表すマップ

	// 変更する候補としてのスケジュールから決まる集計データ
	// スケジュールが決まると確定する量
	private double totalAmountPreserved;	// 動かす前の最大出荷可能量

	// 最適計画から決まる集計データ
	// スケジュールが決まると確定する量
	private double totalAmountBest;		// 最適計画の時の最大出荷可能量

	public static void setData(ShipDays aShipDays, Yield aYield) {
		// 栽培期間，収量，変動費のインスタンスをstatic変数に設定

		shipDays = aShipDays;
		yield = aYield;
;
	}


	public Period(Calendar startDay, double area) {

		fieldArea = area;
		dayAndShipmentOfPeriod = new TreeMap<Calendar, Double>();
		Calendar cal;

		cal = (Calendar)startDay.clone();
		setPeriod(cal);
	}


	public void preserveAndSetPeriod(Calendar startDay) {
		// 現在の日程を保存して新たな日程をセットする．

		preserve();
		setPeriod(startDay);
	}


	public void setPeriod(Calendar startDay) {
		// 生育開始日を設定し，そこから決まる他の日程と総収量，総コストを計算，出荷をリセット．

		Calendar c;

		c = (Calendar) startDay.clone();

		// 生育開始日の設定
		growStart = (Calendar) startDay.clone();

		// 出荷開始日の設定
		c.add(Calendar.DAY_OF_MONTH, shipDays.getGrowDays(startDay) );
		shipStart = (Calendar) c.clone();

		// 出荷終了日の設定
		c.add(Calendar.DAY_OF_MONTH, shipDays.getShipDays(startDay) - 1);
		shipEnd = (Calendar) c.clone();

		// 休耕期間の設定
		fallowPeriod = shipDays.getFallowPeriod(startDay);

		// 後に置く休耕期間も含めたピリオドの終わりの設定
		c.add(Calendar.DATE, fallowPeriod);
		fallowEnd =(Calendar) c.clone();

		calcTotalAmountAndCost(); 		// 総収量と総コストを再計算
		resetShipment();
// 出荷をリセットする（スケジュールが変わると出荷をリセットする必要がある）．
	}


	public void preserve() {
		// ピリオドのスケジュールを変える時に，前の値を保存．

		growStartPreserved = (Calendar) growStart.clone();
		shipStartPreserved = (Calendar) shipStart.clone();
		shipEndPreserved = (Calendar) shipEnd.clone();
		fallowPeriodPreserved = fallowPeriod;
		fallowEndPreserved = (Calendar) fallowEnd.clone();

		totalAmountPreserved = totalAmount;
	}


	public void resetShipment() {
		//growStartの日付をもとに，出荷期間（ShipDays）を求め，dayAndShipmentを初期化
		int sd = shipDays.getShipDays(growStart);
		Calendar c = Calendar.getInstance();
		dayAndShipmentOfPeriod.clear();
		for (int j = 0; j < sd; j++) {
			c = (Calendar) shipStart.clone();
			c.add(Calendar.DAY_OF_MONTH, j);
			dayAndShipmentOfPeriod.put(c, 0.0);
		}

		// 下の3つの量をリセット
		shippedAmount = 0.0;
		restAmount = totalAmount;
	}


	public void revert() {
		// 保存しておいたスケジュールなどの値に戻し，出荷をリセットする．

		growStart = growStartPreserved;
		shipStart = shipStartPreserved;
		shipEnd = shipEndPreserved;
		fallowPeriod = fallowPeriodPreserved;
		fallowEnd = fallowEndPreserved;

		totalAmount = totalAmountPreserved;

		resetShipment();
	}

	public void saveAsBest(){
		// 現状のスケジュールを暫定の最適候補として保存しておく．

		growStartBest = (Calendar) growStart.clone();
		shipStartBest = (Calendar) shipStart.clone();
		shipEndBest = (Calendar) shipEnd.clone();
		fallowPeriodBest = fallowPeriod;
		fallowEndBest = (Calendar) fallowEnd.clone();

		totalAmountBest = totalAmount;
	}

	public void revertBest() {
		// 保存した暫定の最適候補に戻す．

		growStart = growStartBest;
		shipStart = shipStartBest;
		shipEnd = shipEndBest;
		fallowPeriod = fallowPeriodBest;
		fallowEnd = fallowEndBest;

		totalAmount = totalAmountBest;
	}


	public Calendar canShip(Calendar shipDay) {
		// ある日に、このPeriodが出荷可能かどうかを判定して，
		// 出荷可能であれば出荷終了日（のクローン）を返す．
		// 出荷不可能であればnullを返す．

		if ((shipDay.after(shipStart) && shipDay.before(shipEnd)) || shipDay.equals(shipStart)
				|| shipDay.equals(shipEnd)) {
			return (Calendar) shipEnd.clone(); // 呼び出し側で変更しないことが確かなら，clone()要らないが．
		} else {
			return null;
		}
	}


	public Direction changeDateRnd(int days) {
		// このピリオドの栽培開始日を変化させる．
		// -days 〜 +days の間で乱数により変化．

		int changeValue;
		Calendar newDate;

		newDate = (Calendar) growStart.clone();

		while ((changeValue = (int) (Math.random() * days * 2 + 1 - days)) == 0);	//changeValueが0にならないようにランダムに設定

		newDate.add(Calendar.DATE, changeValue);
		//this.changedDays = changeValue;

		return changeDate(newDate);		// 日付が動いた方向を返す．
	}


	private Direction changeDate(Calendar newStartDate) {
		Direction d;

		// 日付が前（BACK）に動いたか，後ろ（FORTH）に動いたか，動かなかったか（STAY）を返す．
		if (newStartDate.before(growStart))
			d = Direction.BACK;
		else if (newStartDate.equals(growStart))
			d = Direction.STAY;
		else if (newStartDate.after(growStart))
			d = Direction.FORTH;
		else
			d = null;

		preserveAndSetPeriod(newStartDate);	// 新しい開始日をセット．
		return d;
	}


	public double getShipment(Calendar date, double shippingAmount) {

		double shipment = 0;				// 出荷量
		if (restAmount >= shippingAmount) {		// 残りの量が要求量よりも大きい時
			shipment = shippingAmount;		// 出荷量は要求量どおりになる．
		} else {
			shipment = restAmount;		// 出荷量は残りの量になる．
		}

		dayAndShipmentOfPeriod.put((Calendar)date.clone(), shipment); // 出荷日・出荷量を記録
		restAmount = restAmount - shipment;
		shippedAmount = shippedAmount + shipment;

		return shipment;
	}


	private void calcTotalAmountAndCost() {
		// 出荷期間内にとれる総収量の計算
		totalAmount = fieldArea * (yield.getYield(shipStart)- yield.getWaste(shipStart));//変更！1/1
	}


	// ーーーーーーーーーーー以下ゲッターーーーーーーーーーー
	public double getShippedAmountOfPeriod() {
		return shippedAmount;
	}


	public double getRestAmountOfPeriod() {
		return restAmount;
	}


	public double getSurplusOfPeriod(Calendar date) {

		if (date.equals(shipEnd)) {
			return restAmount;
		} else {
			return 0.0;
		}

	}


       //----------------------------------------------

	public Calendar getGrowStart() {
		return growStart;
	}

	public Calendar getShipStart() {
		return shipStart;
	}

	public Calendar getShipEnd() {
		return shipEnd;
	}


	public Calendar getFallowEnd() {
		return fallowEnd;
	}


	public double getTotalAmountOfPeriod() {
		return totalAmount;
	}

}

/*
package Optimizer;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class Period {

	// データの格納されたstatic変数
	private static ShipDays shipDays;	// 栽培にかかる日数と出荷期間（日ごとに違う）
	private static Yield yield;	// 収量 [kg/10a]（日ごとに違う），廃棄 [kg/10a]（日ごとに違う）
	private double fieldArea;	// このインスタンスが属する圃場の面積

	// Periodのインスタンスが持つスケジュール
	private Calendar growStart;			// 栽培開始日
	private Calendar shipStart; 			// 出荷開始日
	private Calendar shipEnd;			// 出荷終了日
	private int fallowPeriod;			// このピリオドの後に置く休耕期間
	private Calendar fallowEnd;			// 後に置く休耕期間も含めたピリオドの終わり

	// 変更する候補としてのスケジュール
	private Calendar growStartPreserved;	// 動かす前の栽培開始日
	private Calendar shipStartPreserved;	// 動かす前の出荷開始日
	private Calendar shipEndPreserved;	// 動かす前の出荷終了日
	private int fallowPeriodPreserved;	// 動かす前のピリオドの後に置く休耕期間
	private Calendar fallowEndPreserved;	// 動かす前の（ピリオドの後に置く休耕期間も含めた）ピリオドの終わり

	// 焼きなまし時の暫定最適計画の時のスケジュール
	private Calendar growStartBest;			// 最適計画の時の栽培開始日
	private Calendar shipStartBest;			// 最適計画の時の出荷開始日
	private Calendar shipEndBest;			// 最適計画の時の出荷終了日
	private int fallowPeriodBest;			// 最適計画の時のピリオドの後に置く休耕期間
	private Calendar fallowEndBest;			// 最適計画の時の（ピリオドの後に置く休耕期間も含めた）ピリオドの終わり

	// 確定したスケジュールから決まる集計データ
	// スケジュールが変わるとリセットされる．
	private double totalAmount;								// 最大出荷可能量
	// スケジュールが変わるとリセットされ，出荷に応じて変化する．
	private double shippedAmount;		// 現時点での出荷した総量
	private double restAmount;		// まだ出荷されていない量 totalAmount-shippedAmount
	private Map<Calendar, Double> dayAndShipmentOfPeriod;
// 出荷期間の各日にどれだけ出荷したかを表すマップ
	private double totalCostOfPeriod;		// 全生産コスト(全生産量×生産コスト)

	// 変更する候補としてのスケジュールから決まる集計データ
	// スケジュールが決まると確定する量
	private double totalAmountPreserved;	// 動かす前の最大出荷可能量
	private double totalCostOfPeriodPreserved;	// 動かす前の全生産コスト(全生産量×生産コスト)

	// 最適計画から決まる集計データ
	// スケジュールが決まると確定する量
	private double totalAmountBest;		// 最適計画の時の最大出荷可能量
	private double totalCostOfPeriodBest;	// 最適計画の時の全生産コスト(全生産量×生産コスト)

	public static void setData(ShipDays aShipDays, Yield aYield) {
		// 栽培期間，収量，変動費のインスタンスをstatic変数に設定

		shipDays = aShipDays;
		yield = aYield;
;
	}


	public Period(Calendar startDay, double area) {

		fieldArea = area;
		dayAndShipmentOfPeriod = new TreeMap<Calendar, Double>();
		Calendar cal;

		cal = (Calendar)startDay.clone();
		setPeriod(cal);
	}


	public void preserveAndSetPeriod(Calendar startDay) {
		// 現在の日程を保存して新たな日程をセットする．

		preserve();
		setPeriod(startDay);
	}


	public void setPeriod(Calendar startDay) {
		// 生育開始日を設定し，そこから決まる他の日程と総収量，総コストを計算，出荷をリセット．

		Calendar c;

		c = (Calendar) startDay.clone();

		// 生育開始日の設定
		growStart = (Calendar) startDay.clone();

		// 出荷開始日の設定
		c.add(Calendar.DAY_OF_MONTH, shipDays.getGrowDays(startDay) );
		shipStart = (Calendar) c.clone();

		// 出荷終了日の設定
		c.add(Calendar.DAY_OF_MONTH, shipDays.getShipDays(startDay) - 1);
		shipEnd = (Calendar) c.clone();

		// 休耕期間の設定
		fallowPeriod = shipDays.getFallowPeriod(startDay);

		// 後に置く休耕期間も含めたピリオドの終わりの設定
		c.add(Calendar.DATE, fallowPeriod);
		fallowEnd =(Calendar) c.clone();

		calcTotalAmountAndCost(); 		// 総収量と総コストを再計算
		resetShipment();
// 出荷をリセットする（スケジュールが変わると出荷をリセットする必要がある）．
	}


	public void preserve() {
		// ピリオドのスケジュールを変える時に，前の値を保存．

		growStartPreserved = (Calendar) growStart.clone();
		shipStartPreserved = (Calendar) shipStart.clone();
		shipEndPreserved = (Calendar) shipEnd.clone();
		fallowPeriodPreserved = fallowPeriod;
		fallowEndPreserved = (Calendar) fallowEnd.clone();

		totalAmountPreserved = totalAmount;
		totalCostOfPeriodPreserved = totalCostOfPeriod;
	}


	public void resetShipment() {
		//growStartの日付をもとに，出荷期間（ShipDays）を求め，dayAndShipmentを初期化
		int sd = shipDays.getShipDays(growStart);
		Calendar c = Calendar.getInstance();
		dayAndShipmentOfPeriod.clear();
		for (int j = 0; j < sd; j++) {
			c = (Calendar) shipStart.clone();
			c.add(Calendar.DAY_OF_MONTH, j);
			dayAndShipmentOfPeriod.put(c, 0.0);
		}

		// 下の3つの量をリセット
		shippedAmount = 0.0;
		restAmount = totalAmount;
	}


	public void revert() {
		// 保存しておいたスケジュールなどの値に戻し，出荷をリセットする．

		growStart = growStartPreserved;
		shipStart = shipStartPreserved;
		shipEnd = shipEndPreserved;
		fallowPeriod = fallowPeriodPreserved;
		fallowEnd = fallowEndPreserved;

		totalAmount = totalAmountPreserved;
		totalCostOfPeriod = totalCostOfPeriodPreserved;

		resetShipment();
	}

	public void saveAsBest(){
		// 現状のスケジュールを暫定の最適候補として保存しておく．

		growStartBest = (Calendar) growStart.clone();
		shipStartBest = (Calendar) shipStart.clone();
		shipEndBest = (Calendar) shipEnd.clone();
		fallowPeriodBest = fallowPeriod;
		fallowEndBest = (Calendar) fallowEnd.clone();

		totalAmountBest = totalAmount;
		totalCostOfPeriodBest = totalCostOfPeriod;
	}

	public void revertBest() {
		// 保存した暫定の最適候補に戻す．

		growStart = growStartBest;
		shipStart = shipStartBest;
		shipEnd = shipEndBest;
		fallowPeriod = fallowPeriodBest;
		fallowEnd = fallowEndBest;

		totalAmount = totalAmountBest;
		totalCostOfPeriod = totalCostOfPeriodBest;
	}


	public Calendar canShip(Calendar shipDay) {
		// ある日に、このPeriodが出荷可能かどうかを判定して，
		// 出荷可能であれば出荷終了日（のクローン）を返す．
		// 出荷不可能であればnullを返す．

		if ((shipDay.after(shipStart) && shipDay.before(shipEnd)) || shipDay.equals(shipStart)
				|| shipDay.equals(shipEnd)) {
			return (Calendar) shipEnd.clone(); // 呼び出し側で変更しないことが確かなら，clone()要らないが．
		} else {
			return null;
		}
	}


	public Direction changeDateRnd(int days) {
		// このピリオドの栽培開始日を変化させる．
		// -days 〜 +days の間で乱数により変化．

		int changeValue;
		Calendar newDate;

		newDate = (Calendar) growStart.clone();

		while ((changeValue = (int) (Math.random() * days * 2 + 1 - days)) == 0);	//changeValueが0にならないようにランダムに設定

		newDate.add(Calendar.DATE, changeValue);
		//this.changedDays = changeValue;

		return changeDate(newDate);		// 日付が動いた方向を返す．
	}


	private Direction changeDate(Calendar newStartDate) {
		Direction d;

		// 日付が前（BACK）に動いたか，後ろ（FORTH）に動いたか，動かなかったか（STAY）を返す．
		if (newStartDate.before(growStart))
			d = Direction.BACK;
		else if (newStartDate.equals(growStart))
			d = Direction.STAY;
		else if (newStartDate.after(growStart))
			d = Direction.FORTH;
		else
			d = null;

		preserveAndSetPeriod(newStartDate);	// 新しい開始日をセット．
		return d;
	}


	public double getShipment(Calendar date, double shippingAmount) {

		double shipment = 0;				// 出荷量
		if (restAmount >= shippingAmount) {		// 残りの量が要求量よりも大きい時
			shipment = shippingAmount;		// 出荷量は要求量どおりになる．
		} else {
			shipment = restAmount;		// 出荷量は残りの量になる．
		}

		dayAndShipmentOfPeriod.put((Calendar)date.clone(), shipment); // 出荷日・出荷量を記録
		restAmount = restAmount - shipment;
		shippedAmount = shippedAmount + shipment;

		return shipment;
	}


	private void calcTotalAmountAndCost() {
		// 出荷期間内にとれる総収量の計算
		totalAmount = fieldArea * (yield.getYield(shipStart)- yield.getWaste(shipStart));//変更！1/1
	}


	// ーーーーーーーーーーー以下ゲッターーーーーーーーーーー
	public double getShippedAmountOfPeriod() {
		return shippedAmount;
	}


	public double getRestAmountOfPeriod() {
		return restAmount;
	}


	public double getSurplusOfPeriod(Calendar date) {

		if (date.equals(shipEnd)) {
			return restAmount;
		} else {
			return 0.0;
		}

	}


       //----------------------------------------------

	public Calendar getGrowStart() {
		return growStart;
	}

	public Calendar getShipStart() {
		return shipStart;
	}

	public Calendar getShipEnd() {
		return shipEnd;
	}


	public Calendar getFallowEnd() {
		return fallowEnd;
	}


	public double getTotalAmountOfPeriod() {
		return totalAmount;
	}

}*/
