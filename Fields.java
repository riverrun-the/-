package Optimizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public abstract class Fields {

	ArrayList<Field> fieldList;		// 農業生産法人の持つ圃場のリスト
	Map<Field, Double> shipmentOfEachField;

	public abstract void resetShipment();

	public void addField(Field field) {
		fieldList.add(field);
	}

	public ArrayList<Field> getFieldList() {
		return fieldList;
	}


	public abstract boolean getRandomNeighbor(int days, Calendar dateFrom, int termLength, int maxNumOfChangedFields, int numOfTrial);

	public abstract void revert();

	public abstract double getShipment(Calendar date, double demand);

	public abstract double getSurplus(Calendar date);

	public abstract Map<Field, Double> getShipmentOfEachField();

	public abstract void saveAsBest();

	public abstract void revertBest();

	//==================以下テスト用メソッド==================================
	public abstract void showPlansInDetail();

	public abstract int getNumbersOfChangedField();

}
