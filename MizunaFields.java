package Optimizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MizunaFields extends Fields{

	private ArrayList<Field> changedFields;
	private ArrayList<Integer> changedFieldNumbers;
	public MizunaFields() {
		fieldList = new ArrayList<Field>();
		shipmentOfEachField = new HashMap<Field, Double>();

		changedFields = new ArrayList<Field>();
		changedFieldNumbers = new ArrayList<Integer>();
	}


	public void resetShipment() {
		for (Field field : fieldList) field.resetShipment();
	}


	public boolean getRandomNeighbor(int days, Calendar dateFrom, int termLength, int maxNumOfChangedFields, int numOfTrial) {

		int rnd = -1;
		Field aField;

		if (maxNumOfChangedFields > fieldList.size()) maxNumOfChangedFields = fieldList.size();

		changedFields.clear();
		changedFieldNumbers.clear();
		boolean moved = false;
		for (int i = 0; i < maxNumOfChangedFields; i++) {
			boolean numFlag = true;
			while (numFlag) {
				rnd = (int) (Math.random() * fieldList.size());
				numFlag = changedFieldNumbers.contains(rnd);
			}
			aField = fieldList.get(rnd);
			if ( aField.changePeriodRnd(days, dateFrom, termLength, numOfTrial)){
				changedFieldNumbers.add(rnd);
				changedFields.add(aField);
				moved = true;
			} else {
				System.out.println("Error! In Field, STAY occured.");
			}
		}
		changedFieldNumbers.size();
		return moved;
//動かそうとしたFieldのうち、1つでも動かすことができたらtrue,１つもダメならfalse
	}


	public void revert() {
		for (Field aField : changedFields) aField.revert();
		}

	public void saveAsBest() {
		for (Field field : fieldList) field.saveAsBest();
	}

	public void revertBest() {
		for (Field field : fieldList) field.revertBest();
	}


	public double getShipment(Calendar date, double demand) {
		// ある日(date)にその日の需要量(demand)をできるだけ満たすようにFieldすべてを見渡して出荷を考える．
		// その日の不足量（負の値）を返す．

		ArrayList<Field> availableFields = new ArrayList<Field>();

		double restDemand;
		double shipment;
		double tmpShipment;

		// その日(date)に出荷可能なFieldのインスタンスを探し，出荷終了日の早い順に格納．
		for (Field field : fieldList) {
			if (field.canShip(date) != null) {
				availableFields.add(field);
			}
		}

		// Collections.sortさせる．
		Collections.sort(availableFields, new Comparator<Field>() {
			public int compare(Field f1, Field f2) {
				return f1.canShip(date).compareTo(f2.canShip(date));
			}
		});

		shipment = 0.0;
		restDemand = demand;

		// shipmentOfEachFieldをリセット
		for (Field field : fieldList) shipmentOfEachField.put(field, 0.0);

		for (Field field : availableFields) {
			tmpShipment = field.getShipment(date, restDemand);
			shipment += tmpShipment;
			restDemand -= tmpShipment;
			shipmentOfEachField.put(field, tmpShipment);
			if (shipment >= demand) break;
		}
		return -restDemand;
	}


	public double getSurplus(Calendar date) {
		// ある日(date)の廃棄になった生産過剰量（正の値）を返す．

		double todaysSurplus;

		todaysSurplus = 0.0;
		for (Field field : fieldList) {
			todaysSurplus += field.getSurplusOfField(date);
		}
		return todaysSurplus;
	}


	@Override
	public Map<Field, Double> getShipmentOfEachField() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}


	@Override
	public void showPlansInDetail() {
		// TODO 自動生成されたメソッド・スタブ

	}


	@Override
	public int getNumbersOfChangedField() {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}




}