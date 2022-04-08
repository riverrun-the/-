package Optimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

public class Demand {

	TreeMap<Calendar, Double> thisYearsDemands;
	TreeMap<Calendar, Double> lastYearsDemands;
	TreeMap<Calendar, Double> demandsMapInTerm;
	private double demandSumInTerm;

	public Demand(String thisYearsFilename, String lastYearsFilename) {

		thisYearsDemands = new TreeMap<Calendar, Double>();
		lastYearsDemands = new TreeMap<Calendar, Double>();
		demandsMapInTerm = new TreeMap<Calendar, Double>();

		setDemands(thisYearsDemands, thisYearsFilename);
		setDemands(lastYearsDemands, lastYearsFilename);
	}

	public void setDemands(TreeMap<Calendar, Double> demands, String demandFilename) {
		// 需要量データの読み込み
		// 1行は 「年/月/日, 需要量」の形式で並んでいる．

		DateFormat dateFormat;
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		Date date;

		try {
			File demandFile = new File(demandFilename);
			FileReader demandFileReader = new FileReader(demandFile);
			BufferedReader demandBufferedReader = new BufferedReader(demandFileReader);

			String line = null;
			while ((line = demandBufferedReader.readLine()) != null) {
				String[] items = line.split(",");
				String dateString = items[0];
				date = dateFormat.parse(dateString);
				double demand = Double.parseDouble(items[1]);
				setOneDemand(demands, date, demand);
			}
			demandBufferedReader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void setOneDemand(TreeMap<Calendar, Double> demands, Date date, double demand) {
		// 1日分の需要demandをdemandsに設定
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		demands.put(cal, demand);
	}

	public double getDemand(Calendar today, int leadTime, Calendar date) {
		Calendar leadTimeEnd;
		demandSumInTerm = 0.0;
		if (!checkBoundary(thisYearsDemands, date) || !checkBoundary(lastYearsDemands, date)) {
			System.out.println("Demand Boundary Error\n");
			System.exit(1);
		}

		leadTimeEnd = (Calendar) today.clone();
		leadTimeEnd.add(Calendar.DATE, leadTime);
		if (date.after(leadTimeEnd) || date.equals(leadTimeEnd)) {
			return lastYearsDemands.get(date);
		} else {
			return thisYearsDemands.get(date);
		}
	}

	public double getThisYearsDemand(Calendar date) {
		Calendar cal;
		cal = (Calendar) date.clone();
		return thisYearsDemands.get(cal);
	}

	public double getLastYearsDemand(Calendar date) {
		Calendar cal;
		cal = (Calendar) date.clone();
		return lastYearsDemands.get(cal);
	}

	public TreeMap<Calendar, Double> getDemandMap(Calendar today, int leadTime, int termLength) {

		demandSumInTerm = 0.0;
		demandsMapInTerm.clear();

		Calendar date;
		Calendar leadTimeEnd;
		leadTimeEnd = (Calendar) today.clone();
		leadTimeEnd.add(Calendar.DATE, leadTime);

		Calendar TermEnd;
		TermEnd = (Calendar) today.clone();
		TermEnd.add(Calendar.DATE, termLength);

		for (date = (Calendar) today.clone(); !date.equals(leadTimeEnd); date.add(Calendar.DATE, 1)) {
			demandsMapInTerm.put((Calendar) date.clone(), thisYearsDemands.get(date));
			demandSumInTerm += thisYearsDemands.get(date);
		}

		for (date = (Calendar) leadTimeEnd.clone(); !date.after(TermEnd); date.add(Calendar.DATE, 1)) {
			demandsMapInTerm.put((Calendar) date.clone(), lastYearsDemands.get(date));
			demandSumInTerm += lastYearsDemands.get(date);
		}

		return demandsMapInTerm;
	}

	private boolean checkBoundary(TreeMap<Calendar, Double> demands, Calendar date) {
		return (!(date.before((demands.firstKey())) && !(date.after(demands.lastKey()))));
	}

	public double getDemandSumInTerm() {
		return demandSumInTerm;
	}
}
