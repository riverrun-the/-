package Optimizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class TestSimulation {

	// Period, Field, Fields, Optimizer, Simulation, Demands, ShipDays, Yields, VariableCostの
	// インスタンスを生成する．
	// インスタンス生成時に必要なデータのファイル名をコンストラクタの引数として与える．
	// Simulationクラスにシミュレーションを行わせる．

	public static void main(String[] args) {

		ShipDays shipDays;
		Yield yield;
		// ファイル名の変数
		String shipDaysFilename;// 栽培開始日に対する，栽培期間，出荷期間，休耕期間のファイル．
		String yieldFilename;	// 栽培開始日に対する，収量と廃棄量のファイル．
		String fieldFilename;		// フィールド（ハウス）の番号と面積のファイル．
		String periodFilename;// 各ピリオドが属するフィールド番号と栽培開始日のファイル．
		String thisYearsDemandFilename;// 今年の需要量のファイル．
		String lastYearsDemandFilename; // 去年の需要量のファイル．
		// ファイル名の設定
		shipDaysFilename = "みずなの栽培・出荷・休耕期間データ2.csv";
		yieldFilename = "みずなの収量・廃棄量データ.csv";
		fieldFilename = "Field初期設定データ2.csv";
		periodFilename = "Period初期設定データ5.csv";
		thisYearsDemandFilename = "今年の需要量15日おき矩形波(120-420).csv";
		lastYearsDemandFilename = "去年の需要量15日おき矩形波(120-420).csv";
		// シミュレーションの開始日と終了日
		String startDayString;
		String endDayString;
		startDayString = "2015/2/1";
		endDayString = "2016/3/31";

		// シミュレーション条件
		int leadTime;		// オーダーの情報リードタイム
		double t;			// 初期温度
		double ratio;		// 温度を更新する比
		int rangeOfChangeDate;	// 乱数でPeriodを動かす日数の上限
		int maxNumOfChangedFields;	// 乱数で動かすFieldの数の上限
		int numOfTrial;		// あるField内のPeriodを動かす時の試行回数の上限

		leadTime =41;
		t = 1E+50;
		ratio = 0.95;
		rangeOfChangeDate = 10;
		maxNumOfChangedFields = 5;
		numOfTrial = 10;


		// 文字列→日付変換用
		DateFormat dateFormat;
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);


		// 収穫期間データ，収量・廃棄量データ，変動費データのインスタンスを生成
		shipDays = new ShipDays(shipDaysFilename);
		yield = new Yield(yieldFilename);

		// Fieldのインスタンスをファイルデータをもとに生成
		TreeMap<Integer, Field> fieldMap = new TreeMap<Integer, Field>();
		Field aField;

			try {
				File fieldFile = new File(fieldFilename);
				FileReader fieldFileReader = new FileReader(fieldFile);
				BufferedReader fieldBufferedReader = new BufferedReader(fieldFileReader);

				String line = null;
				while ((line = fieldBufferedReader.readLine()) != null) {
					String [] items = line.split(",");

					int fieldNumber = Integer.parseInt(items[0]);
					double area = Double.parseDouble(items[1]);
					aField = new Field(fieldNumber, area);
					fieldMap.put(fieldNumber,  aField);
				}
				fieldBufferedReader.close();

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		// Periodクラスに収穫期間データ，収量・廃棄量データ，変動費データをstatic変数としてset
		Period.setData(shipDays, yield);

		// Periodのインスタンスをファイルデータをもとに生成．
		// 生成したPeriodのインスタンスをFieldのインスタンスにaddする．
		Period aPeriod;
		Calendar cal;

			try {
				File periodFile = new File(periodFilename);
				FileReader periodFileReader = new FileReader(periodFile);
				BufferedReader periodFileBufferedReader = new BufferedReader(periodFileReader);

				String line = null;
				while ((line = periodFileBufferedReader.readLine()) != null) {
					String [] items = line.split(",");

					int fieldNumber = Integer.parseInt(items[0]);
					int size = items.length;
					for (int i = 1; i < size; i++) {
						cal = Calendar.getInstance();
						cal.setTime(dateFormat.parse(items[i]));

						aField = fieldMap.get(fieldNumber);
						aPeriod = new Period(cal, aField.getArea());
						aField.addPeriod(aPeriod);
					}
				}
				periodFileBufferedReader.close();

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		// Fieldsのインスタンスを生成して，Fieldのインスタンス（fieldMapの中のインスタンス）をaddする．
		Fields fields = new MizunaFields();

		for (Field f : fieldMap.values()) {
			fields.addField(f);
		}

		// シミュレーションの開始日と終了日
		Calendar startDay;
		Calendar endDay;

		// Stringの日付をCalendarに変換．
		startDay = Calendar.getInstance();
		endDay = Calendar.getInstance();

			try {
				startDay.setTime(dateFormat.parse(startDayString));
				endDay.setTime(dateFormat.parse(endDayString));
			} catch(Exception ex) {
				ex.printStackTrace();
			}

		// 差を日数で表す．
		//long termLengthLong = endDay.getTimeInMillis() - startDay.getTimeInMillis();
		//termLengthLong = TimeUnit.MILLISECONDS.toDays(termLengthLong);
		// int termLength = (int) (termLengthLong + 1);
		int termLength = 365;
		// 需要量の設定
		Demand demands = new Demand(thisYearsDemandFilename, lastYearsDemandFilename);

		// 評価関数で使用する評価の方法
		Evaluation evaluation;
		evaluation = new EvaluationWithPenalty(3);

		// Optimizerインスタンスの生成
		Optimizer.Builder optBuilder = new Optimizer.Builder(fields, demands, evaluation, startDay);
		Optimizer optimizer = optBuilder.termLength(termLength)
										.leadTime(leadTime)
										.t(t)
										.ratio(ratio)
										.rangeOfChangeDate(rangeOfChangeDate)
										.maxNumOfChangedFields(maxNumOfChangedFields)
										.numOfTrial(numOfTrial)
										.build();

		// Simulation インスタンスの生成
		System.out.println("Start Day: " + startDay.getTime() + ",  End Day: " + endDay.getTime());
		Simulation simulation = new Simulation(optimizer, startDay, endDay, 1, demands, leadTime);

		// シミュレーションを開始する．
		long start = System.nanoTime();			//時間計測スタート

			System.out.println("Start Simulation.");
			simulation.optimizeWithOrders();	// シミュレーションの実行．

		long nanoSeconds = System.nanoTime() - start;
		long totalSeconds = TimeUnit.NANOSECONDS.toSeconds(nanoSeconds);
		int hours = (int)(totalSeconds / 3600);
		int minutes = (int)((totalSeconds % 3600) / 60);
		int seconds = (int)(totalSeconds % 60);
		System.out.printf("処理時間：%d時間%d分%d秒　（総秒数：%d秒）\n", hours, minutes, seconds, totalSeconds);


		// 実際の出荷を行い，日付とそれに対応した実需に対する生産過剰量（正），不足量（負）のTreeMapを得る．
		String actualShipStartDayString;
		actualShipStartDayString = "2015/4/1";
		Calendar ActualShipStartDay;
		ActualShipStartDay = Calendar.getInstance();
		String actualShipEndDayString;
		actualShipEndDayString = "2016/3/31";
		Calendar ActualShipEndDay;
		ActualShipEndDay = Calendar.getInstance();
		try {
			ActualShipStartDay.setTime(dateFormat.parse(actualShipStartDayString));
			ActualShipEndDay.setTime(dateFormat.parse(actualShipEndDayString));
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		double evalValue = simulation.getActualShipmentForTerm(ActualShipStartDay, ActualShipEndDay);
		System.out.println("評価関数 : "+(int) evalValue);
	}

}
