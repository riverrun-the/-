package Optimizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

public class Yield {

	TreeMap<Calendar,Double> yieldsMap;   //日付とその日の収穫量（kg/10a）を格納したMap
	TreeMap<Calendar,Double> wasteMap;   //日付とその日の廃棄量（kg/10a）を格納したMap

	public Yield(String yieldAndWasteDataFilename) {
		yieldsMap=new TreeMap<Calendar,Double>();
		wasteMap=new TreeMap<Calendar,Double>();

		setYieldAndWasteData(yieldAndWasteDataFilename);
	}


	public void setYieldAndWasteData(String yieldAndWasteDataFilename) {
		// 収量，廃棄量データの読み込み
		// 1行は　「1972/月/日, 収量，廃棄量」の形式で並んでいる（年は関係無い）．
		// 2月29日のデータも用意しておくこと．

		DateFormat dateFormat;
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		Date date;

		try {
			File yieldAndWasteDataFile = new File(yieldAndWasteDataFilename);
			FileReader yieldAndWasteDataFileReader = new FileReader(yieldAndWasteDataFile);
			BufferedReader yieldAndWasteDataBufferedReader = new BufferedReader(yieldAndWasteDataFileReader);

			String line = null;
			while ((line = yieldAndWasteDataBufferedReader.readLine()) != null) {
				String [] items = line.split(",");

				String dateString = items[0];
				date = dateFormat.parse(dateString);

				double yield = Double.parseDouble(items[1])*1000;
				double waste = Double.parseDouble(items[2])*1000;

				setOneYieldOrWasteData(yieldsMap, date, yield);
				setOneYieldOrWasteData(wasteMap, date, waste);

			}
			yieldAndWasteDataBufferedReader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	private void setOneYieldOrWasteData(TreeMap<Calendar,Double> yieldsOrWasteMap, Date date, double amount) {
		// ある栽培開始日（date）で栽培した場合の収量または廃棄量データ（amount）を設定．
		// 毎年同じデータを使うことにしたので，1年分のデータだけ用意．
		// ただし閏年に対応するために2月29日のデータも必要となるので，閏年の1972でセットしている．

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.YEAR, 1972);		// 1972はCalendarで設定できる一番早い閏年
		yieldsOrWasteMap.put(cal, amount);
	}


	public double getYield(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return yieldsMap.get(c);
	}


	public double getWaste(Calendar cal){
		Calendar c;

		c = (Calendar) cal.clone();
		c.set(Calendar.YEAR, 1972);
		return wasteMap.get(c);
	}
}
