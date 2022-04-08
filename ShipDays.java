package Optimizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;


public class ShipDays {


	TreeMap<Calendar,Integer> growPeriods;     // 生育期間
	TreeMap<Calendar,Integer> shipPeriods;      // 出荷期間
	TreeMap<Calendar,Integer> fallowPeriods;    // 休耕期間


	public ShipDays(String periodDataFilename) {

		growPeriods = new TreeMap<Calendar,Integer>();
		shipPeriods = new TreeMap<Calendar,Integer>();
		fallowPeriods= new TreeMap<Calendar,Integer>();

		setPeriodData(periodDataFilename);
	}

	public void setPeriodData(String periodDataFilename) {
		// ピリオドデータの読み込み
		// 1行は　「1972/月/日, 生育期間，出荷期間，休耕期間」の形式で並んでいる（年は関係無い）
		// 2月29日のデータも用意しておくこと．




		DateFormat dateFormat;
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		Date date;


		try {
			File periodDataFile = new File(periodDataFilename);
			FileReader periodDataFileReader = new FileReader(periodDataFile);
			BufferedReader periodDataBufferedReader = new BufferedReader(periodDataFileReader);


			String line = null;
			while ((line = periodDataBufferedReader.readLine()) != null) {
				String [] items = line.split(",");

				String dateString = items[0];
				date = dateFormat.parse(dateString);

				int growPeriod = Integer.parseInt(items[1]);
				int shipPeriod = Integer.parseInt(items[2]);
				int fallowPeriod = Integer.parseInt(items[3]);

				setOnePeriodData(growPeriods, date, growPeriod);
				setOnePeriodData(shipPeriods, date, shipPeriod);
				setOnePeriodData(fallowPeriods, date, fallowPeriod);
			}
			periodDataBufferedReader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void setOnePeriodData(TreeMap<Calendar,Integer> periodData, Date date, int days) {
	//private void setOnePeriodData(TreeMap<Calendar,Integer> periodData, Date date, int days) {
		// 生育期間，出荷期間，休耕期間のいずれか（periodData）に
		// ある栽培開始日（date）におけるデータ（days）を設定．
		// 毎年同じデータを使うことにしたので，1年分のデータだけ用意．
		// ただし閏年に対応するために2月29日のデータも必要となるので，閏年の1972でセットしている．

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.YEAR, 1972);		// 1972はCalendarで設定できる一番早い閏年
		periodData.put(cal, days);
	}


	public int getGrowDays(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return growPeriods.get(c);
	}


	public int getShipDays(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return shipPeriods.get(c);
	}


	public int getFallowPeriod(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return fallowPeriods.get(c);
	}
}

/*
package Optimizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;


public class ShipDays {




	TreeMap<Calendar,Integer> growPeriods;     // 生育期間
	TreeMap<Calendar,Integer> shipPeriods;      // 出荷期間
	TreeMap<Calendar,Integer> fallowPeriods;    // 休耕期間


	public ShipDays(String periodDataFilename) {

		growPeriods = new TreeMap<Calendar,Integer>();
		shipPeriods = new TreeMap<Calendar,Integer>();
		fallowPeriods= new TreeMap<Calendar,Integer>();

		setPeriodData(periodDataFilename);
	}

	public void setPeriodData(String periodDataFilename) {
		// ピリオドデータの読み込み
		// 1行は　「1972/月/日, 生育期間，出荷期間，休耕期間」の形式で並んでいる（年は関係無い）
		// 2月29日のデータも用意しておくこと．




		DateFormat dateFormat;
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		Date date;


		try {
			File periodDataFile = new File(periodDataFilename);
			FileReader periodDataFileReader = new FileReader(periodDataFile);
			BufferedReader periodDataBufferedReader = new BufferedReader(periodDataFileReader);


			String line = null;
			while ((line = periodDataBufferedReader.readLine()) != null) {
				String [] items = line.split(",");

				String dateString = items[0];
				date = dateFormat.parse(dateString);

				int growPeriod = Integer.parseInt(items[1]);
				int shipPeriod = Integer.parseInt(items[2]);
				int fallowPeriod = Integer.parseInt(items[3]);

				setOnePeriodData(growPeriods, date, growPeriod);
				setOnePeriodData(shipPeriods, date, shipPeriod);
				setOnePeriodData(fallowPeriods, date, fallowPeriod);
			}
			periodDataBufferedReader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void setOnePeriodData(TreeMap<Calendar,Integer> periodData, Date date, int days) {
	//private void setOnePeriodData(TreeMap<Calendar,Integer> periodData, Date date, int days) {
		// 生育期間，出荷期間，休耕期間のいずれか（periodData）に
		// ある栽培開始日（date）におけるデータ（days）を設定．
		// 毎年同じデータを使うことにしたので，1年分のデータだけ用意．
		// ただし閏年に対応するために2月29日のデータも必要となるので，閏年の1972でセットしている．

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.YEAR, 1972);		// 1972はCalendarで設定できる一番早い閏年
		periodData.put(cal, days);
	}


	public int getGrowDays(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return growPeriods.get(c);
	}


	public int getShipDays(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return shipPeriods.get(c);
	}


	public int getFallowPeriod(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return fallowPeriods.get(c);
	}
}*/
